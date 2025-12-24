<?php
// Turn off error display and capture errors - MUST be first
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Start output buffering to catch any unwanted output
ob_start();

// Initialize connection variable
$conn = null;

// Register shutdown function to ensure connection is always closed
register_shutdown_function(function() use (&$conn) {
    if (isset($conn) && $conn !== null) {
        try {
            $conn->close();
        } catch (Exception $e) {
            // Ignore
        } catch (Error $e) {
            // Ignore
        }
    }
});

// Try to include connection file with proper error handling
try {
    include "conn.php";
} catch (mysqli_sql_exception $e) {
    ob_clean();
    header('Content-Type: application/json');
    $errorMsg = $e->getMessage();
    if (strpos($errorMsg, 'max_connections_per_hour') !== false) {
        $errorMsg = "Database connection limit exceeded. Please try again later.";
    }
    echo json_encode([
        "status" => 0,
        "error" => $errorMsg
    ]);
    ob_end_flush();
    exit;
} catch (Exception $e) {
    ob_clean();
    header('Content-Type: application/json');
    echo json_encode([
        "status" => 0,
        "error" => "Failed to load database connection: " . $e->getMessage()
    ]);
    ob_end_flush();
    exit;
} catch (Error $e) {
    ob_clean();
    header('Content-Type: application/json');
    echo json_encode([
        "status" => 0,
        "error" => "Failed to load database connection: " . $e->getMessage()
    ]);
    ob_end_flush();
    exit;
}

// Check if connection failed or wasn't set
if (!isset($conn) || $conn === null || (property_exists($conn, 'connect_error') && $conn->connect_error)) {
    ob_clean();
    header('Content-Type: application/json');
    $errorMsg = "Database connection failed";
    if (isset($conn) && $conn !== null && property_exists($conn, 'connect_error')) {
        $errorMsg .= ": " . $conn->connect_error;
        // Check if it's a connection limit error
        if (strpos($conn->connect_error, 'max_connections_per_hour') !== false) {
            $errorMsg = "Database connection limit exceeded. Please try again later.";
        }
    } else {
        $errorMsg .= ": Connection object not set";
    }
    echo json_encode([
        "status" => 0,
        "error" => $errorMsg
    ]);
    ob_end_flush();
    exit;
}

header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

// Clear any output that might have been generated
ob_clean();

$user_id = $_POST['user_id'] ?? '';
$since = isset($_POST['since']) ? intval($_POST['since']) : 0; // Timestamp to fetch notifications after

if (empty($user_id)) {
    echo json_encode(["status" => 0, "error" => "Missing user_id"]);
    ob_end_flush();
    if (isset($conn) && $conn !== null) {
        try { $conn->close(); } catch (Exception $e) {}
    }
    exit;
}

$notifications = [];
$errors = [];

// 1. Fetch unread messages (where user is receiver and not seen)
try {
    $msgQuery = "SELECT m.*, COALESCE(u.username, 'Someone') as sender_name
                 FROM messages m 
                 LEFT JOIN users u ON m.sender_id = u.uid
                 WHERE m.receiver_id = ? AND m.seen = 0 AND m.deleted = 0";
    if ($since > 0) {
        $msgQuery .= " AND m.timestamp > ?";
    }

    $msgStmt = $conn->prepare($msgQuery);
    if (!$msgStmt) {
        $errorMsg = $conn->error ?? "Unknown prepare error";
        $errors[] = "Message query prepare failed: " . $errorMsg;
        error_log("Notification fetch - Message query prepare error: " . $errorMsg);
    } else {
        if ($since > 0) {
            // Use 'i' for integer, but ensure $since is within valid range
            // Timestamps in milliseconds can be very large, so we need to handle them as integers
            if (!$msgStmt->bind_param("si", $user_id, $since)) {
                $errors[] = "Message query bind_param failed: " . $msgStmt->error;
                error_log("Notification fetch - bind_param error: " . $msgStmt->error);
            }
        } else {
            if (!$msgStmt->bind_param("s", $user_id)) {
                $errors[] = "Message query bind_param failed: " . $msgStmt->error;
                error_log("Notification fetch - bind_param error: " . $msgStmt->error);
            }
        }
        if (!empty($errors) && strpos(end($errors), "bind_param") !== false) {
            // Skip execution if bind failed
        } else if (!$msgStmt->execute()) {
            $errorMsg = $msgStmt->error ?? "Unknown execute error";
            $errors[] = "Message query execute failed: " . $errorMsg;
            error_log("Notification fetch - Message query execute error: " . $errorMsg);
        } else {
            $msgResult = $msgStmt->get_result();
            if ($msgResult) {
                while ($row = $msgResult->fetch_assoc()) {
                    $notifications[] = [
                        "notification_id" => "msg_" . $row['message_id'],
                        "type" => "message",
                        "chat_id" => $row['chat_id'],
                        "sender_id" => $row['sender_id'],
                        "sender_name" => $row['sender_name'] ?? "Someone",
                        "text" => $row['text'] ?? "",
                        "has_image" => !empty($row['image']),
                        "has_post" => !empty($row['post_id']),
                        "timestamp" => intval($row['timestamp'] ?? 0)
                    ];
                }
                $msgResult->free();
            }
        }
        $msgStmt->close();
    }
} catch (Exception $e) {
    $errorMsg = $e->getMessage();
    $errors[] = "Message query exception: " . $errorMsg;
    error_log("Notification fetch - Message query exception: " . $errorMsg . " | Trace: " . $e->getTraceAsString());
} catch (Error $e) {
    $errorMsg = $e->getMessage();
    $errors[] = "Message query fatal error: " . $errorMsg;
    error_log("Notification fetch - Message query fatal error: " . $errorMsg . " | Trace: " . $e->getTraceAsString());
}

// 2. Fetch pending follow requests (where user is being followed)
try {
    $followQuery = "SELECT f.*, 
                    COALESCE(u.username, 'Someone') as follower_name
                    FROM follows f 
                    LEFT JOIN users u ON f.follower_id = u.uid
                    WHERE f.following_id = ? AND f.status = 'pending'";

    $followStmt = $conn->prepare($followQuery);
    if (!$followStmt) {
        $errorMsg = $conn->error ?? "Unknown prepare error";
        $errors[] = "Follow query prepare failed: " . $errorMsg;
        error_log("Notification fetch - Follow query prepare error: " . $errorMsg);
    } else {
        if (!$followStmt->bind_param("s", $user_id)) {
            $errors[] = "Follow query bind_param failed: " . $followStmt->error;
            error_log("Notification fetch - Follow bind_param error: " . $followStmt->error);
        } else if (!$followStmt->execute()) {
            $errorMsg = $followStmt->error ?? "Unknown execute error";
            $errors[] = "Follow query execute failed: " . $errorMsg;
            error_log("Notification fetch - Follow query execute error: " . $errorMsg);
        } else {
            $followResult = $followStmt->get_result();
            if ($followResult) {
                while ($row = $followResult->fetch_assoc()) {
                    // Determine timestamp - adjust based on your schema
                    $timestamp = time() * 1000; // Default to current time
                    if (isset($row['timestamp']) && is_numeric($row['timestamp'])) {
                        $timestamp = intval($row['timestamp']);
                    } elseif (isset($row['created_at'])) {
                        $timestamp = strtotime($row['created_at']) * 1000;
                    }
                    
                    $notifications[] = [
                        "notification_id" => "req_" . $row['follower_id'],
                        "type" => "follow_request",
                        "follower_id" => $row['follower_id'],
                        "follower_name" => $row['follower_name'] ?? "Someone",
                        "timestamp" => $timestamp
                    ];
                }
                $followResult->free();
            }
        }
        $followStmt->close();
    }
} catch (Exception $e) {
    $errorMsg = $e->getMessage();
    $errors[] = "Follow query exception: " . $errorMsg;
    error_log("Notification fetch - Follow query exception: " . $errorMsg . " | Trace: " . $e->getTraceAsString());
} catch (Error $e) {
    $errorMsg = $e->getMessage();
    $errors[] = "Follow query fatal error: " . $errorMsg;
    error_log("Notification fetch - Follow query fatal error: " . $errorMsg . " | Trace: " . $e->getTraceAsString());
}

// 3. Fetch screenshot alerts (where user is recipient)
try {
    $shotQuery = "SELECT s.*, 
                  COALESCE(u.username, 'Someone') as sender_name
                  FROM screenshots s 
                  LEFT JOIN users u ON s.by_user = u.uid
                  WHERE s.to_user = ?";
    if ($since > 0) {
        $shotQuery .= " AND s.timestamp > ?";
    }

    $shotStmt = $conn->prepare($shotQuery);
    if (!$shotStmt) {
        $errorMsg = $conn->error ?? "Unknown prepare error";
        $errors[] = "Screenshot query prepare failed: " . $errorMsg;
        error_log("Notification fetch - Screenshot query prepare error: " . $errorMsg);
    } else {
        if ($since > 0) {
            if (!$shotStmt->bind_param("si", $user_id, $since)) {
                $errors[] = "Screenshot query bind_param failed: " . $shotStmt->error;
                error_log("Notification fetch - Screenshot bind_param error: " . $shotStmt->error);
            }
        } else {
            if (!$shotStmt->bind_param("s", $user_id)) {
                $errors[] = "Screenshot query bind_param failed: " . $shotStmt->error;
                error_log("Notification fetch - Screenshot bind_param error: " . $shotStmt->error);
            }
        }
        if (!empty($errors) && strpos(end($errors), "bind_param") !== false) {
            // Skip execution if bind failed
        } else if (!$shotStmt->execute()) {
            $errorMsg = $shotStmt->error ?? "Unknown execute error";
            $errors[] = "Screenshot query execute failed: " . $errorMsg;
            error_log("Notification fetch - Screenshot query execute error: " . $errorMsg);
        } else {
            $shotResult = $shotStmt->get_result();
            if ($shotResult) {
                while ($row = $shotResult->fetch_assoc()) {
                    $notifications[] = [
                        "notification_id" => "shot_" . $row['screenshot_id'],
                        "type" => "screenshot",
                        "chat_id" => $row['chat_id'],
                        "sender_id" => $row['by_user'],
                        "sender_name" => $row['sender_name'] ?? "Someone",
                        "timestamp" => intval($row['timestamp'] ?? 0)
                    ];
                }
                $shotResult->free();
            }
        }
        $shotStmt->close();
    }
} catch (Exception $e) {
    $errorMsg = $e->getMessage();
    $errors[] = "Screenshot query exception: " . $errorMsg;
    error_log("Notification fetch - Screenshot query exception: " . $errorMsg . " | Trace: " . $e->getTraceAsString());
} catch (Error $e) {
    $errorMsg = $e->getMessage();
    $errors[] = "Screenshot query fatal error: " . $errorMsg;
    error_log("Notification fetch - Screenshot query fatal error: " . $errorMsg . " | Trace: " . $e->getTraceAsString());
}

// Sort by timestamp descending (newest first)
usort($notifications, function($a, $b) {
    return ($b['timestamp'] ?? 0) - ($a['timestamp'] ?? 0);
});

// Clear any output buffer before sending JSON
ob_clean();

// If we have critical errors and no notifications, return error status
if (!empty($errors) && empty($notifications)) {
    // Check if any errors are critical (not just warnings)
    $criticalErrors = array_filter($errors, function($err) {
        return strpos($err, "prepare failed") !== false || 
               strpos($err, "execute failed") !== false ||
               strpos($err, "fatal error") !== false;
    });
    
    if (!empty($criticalErrors)) {
        echo json_encode([
            "status" => 0,
            "error" => "Failed to fetch notifications",
            "errors" => $errors
        ]);
        ob_end_flush();
        if (isset($conn) && $conn !== null) {
            try { $conn->close(); } catch (Exception $e) {}
        }
        exit;
    }
}

$response = [
    "status" => 1,
    "notifications" => $notifications,
    "count" => count($notifications),
    "server_time" => time() * 1000
];

if (!empty($errors)) {
    $response["errors"] = $errors;
    // Log errors but don't fail the request if we got some notifications
    error_log("Notification fetch errors: " . implode(", ", $errors));
}

// Ensure we output valid JSON
$jsonResponse = json_encode($response);
if ($jsonResponse === false) {
    // If JSON encoding fails, return error
    ob_clean();
    echo json_encode([
        "status" => 0,
        "error" => "Failed to encode response",
        "json_error" => json_last_error_msg()
    ]);
} else {
    echo $jsonResponse;
}

// End output buffering and send
ob_end_flush();

// Always close connection, even if there were errors
if (isset($conn) && $conn !== null) {
    try {
        $conn->close();
    } catch (Exception $e) {
        // Ignore close errors
    } catch (Error $e) {
        // Ignore close errors
    }
}
?>
