<?php
/**
 * messages_delete_vanish.php
 *
 * This endpoint deletes vanish mode messages when a receiver closes the chat.
 * It only deletes messages where:
 * - vanish_mode = 1
 * - receiver_id matches the requesting user (so they only delete messages they received)
 *
 * Upload this file to your server at: http://sociallyah.atwebpages.com/
 */

header('Content-Type: application/json');

// Database connection - update with your credentials
$servername = "your_server";
$username = "your_username";
$password = "your_password";
$dbname = "your_database";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    echo json_encode([
        'status' => 0,
        'message' => 'Database connection failed'
    ]);
    exit;
}

// Get parameters
$chat_id = isset($_POST['chat_id']) ? trim($_POST['chat_id']) : '';
$receiver_id = isset($_POST['receiver_id']) ? trim($_POST['receiver_id']) : '';

// Validate inputs
if (empty($chat_id) || empty($receiver_id)) {
    echo json_encode([
        'status' => 0,
        'message' => 'Missing required parameters'
    ]);
    $conn->close();
    exit;
}

// Prepare statement to delete vanish mode messages
// Only delete messages where the current user is the receiver
$stmt = $conn->prepare("DELETE FROM messages WHERE chat_id = ? AND vanish_mode = 1 AND receiver_id = ?");
$stmt->bind_param("ss", $chat_id, $receiver_id);

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
        'message' => 'Failed to delete vanish mode messages',
        'error' => $stmt->error
    ]);
}

$stmt->close();
$conn->close();
?>

