<?php
/**
 * messages_vanish_delete.php
 * Deletes vanish mode messages when a user closes the chat
 */

// Include your database connection file
include "conn.php";

// Set response header
header('Content-Type: application/json');

// Get POST parameters
$chat_id = isset($_POST['chat_id']) ? trim($_POST['chat_id']) : '';
$user_id = isset($_POST['user_id']) ? trim($_POST['user_id']) : '';

// Validate inputs
if (empty($chat_id) || empty($user_id)) {
    echo json_encode([
        'status' => 0,
        'message' => 'Missing required parameters',
        'deleted_count' => 0
    ]);
    exit;
}

// Prepare and execute delete statement
// Delete messages where:
// 1. chat_id matches
// 2. vanish_mode = 1
// 3. seen = 1 (message was viewed)
// 4. receiver_id = user_id (only delete messages received by this user, not sent by them)
$stmt = $conn->prepare("DELETE FROM messages WHERE chat_id = ? AND vanish_mode = 1 AND seen = 1 AND receiver_id = ?");

if (!$stmt) {
    echo json_encode([
        'status' => 0,
        'message' => 'Database error: ' . $conn->error,
        'deleted_count' => 0
    ]);
    exit;
}

$stmt->bind_param("ss", $chat_id, $user_id);

if ($stmt->execute()) {
    $deleted_count = $stmt->affected_rows;
    echo json_encode([
        'status' => 1,
        'message' => 'Vanish mode messages deleted successfully',
        'deleted_count' => $deleted_count
    ]);
} else {
    echo json_encode([
        'status' => 0,
        'message' => 'Failed to delete vanish mode messages: ' . $stmt->error,
        'deleted_count' => 0
    ]);
}

$stmt->close();
$conn->close();
?>

