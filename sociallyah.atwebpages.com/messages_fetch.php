<?php
include "conn.php";

// CRITICAL: Prevent caching to ensure real-time message sync across devices
header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$chat_id = $_POST['chat_id'] ?? '';
$user_id = $_POST['user_id'] ?? '';
$since = isset($_POST['since']) ? intval($_POST['since']) : 0;

if (empty($chat_id)) {
    echo json_encode(["status" => 0, "error" => "Missing chat_id"]);
    exit;
}

// Debug logging (remove in production or use error_log)
error_log("messages_fetch.php: chat_id=$chat_id, user_id=$user_id");

// Fetch all messages for this chat, ordered by timestamp
// Also try reverse order in case chat_id format differs (userA_userB vs userB_userA)
$q = "SELECT * FROM messages WHERE chat_id = ? OR chat_id = ? ORDER BY timestamp ASC";
$stmt = $conn->prepare($q);

// Try both orders: original and reversed
$parts = explode("_", $chat_id);
$reversed_chat_id = count($parts) == 2 ? $parts[1] . "_" . $parts[0] : $chat_id;

$stmt->bind_param("ss", $chat_id, $reversed_chat_id);
$stmt->execute();
$result = $stmt->get_result();

// Debug: log how many messages found
$message_count = $result->num_rows;
error_log("messages_fetch.php: Found $message_count messages for chat_id=$chat_id (also checked reversed: $reversed_chat_id)");

$messages = [];
while ($row = $result->fetch_assoc()) {
    // Ensure all fields are present in response
    $messages[] = [
        "message_id" => $row['message_id'] ?? '',
        "chat_id" => $row['chat_id'] ?? '',
        "sender_id" => $row['sender_id'] ?? '',
        "receiver_id" => $row['receiver_id'] ?? '',
        "text" => $row['text'] ?? '',
        "image" => $row['image'] ?? '',
        "post_id" => $row['post_id'] ?? '',
        "vanish_mode" => intval($row['vanish_mode'] ?? 0),
        "seen" => intval($row['seen'] ?? 0),
        "deleted" => intval($row['deleted'] ?? 0),
        "edited" => intval($row['edited'] ?? 0),
        "timestamp" => intval($row['timestamp'] ?? 0)
    ];
}

echo json_encode([
    "status" => 1, 
    "messages" => $messages,
    "count" => count($messages),
    "server_time" => time() * 1000
]);

$stmt->close();
$conn->close();
?>
