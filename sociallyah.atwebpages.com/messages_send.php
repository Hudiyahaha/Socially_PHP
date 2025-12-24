<?php
include "conn.php";

// Prevent caching
header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$sender = $_POST['sender_id'] ?? '';
$receiver = $_POST['receiver_id'] ?? '';
$chat_id = $_POST['chat_id'] ?? '';
$text = $_POST['text'] ?? "";
$image = $_POST['image'] ?? "";
$post_id = $_POST['post_id'] ?? "";
$vanish_mode = isset($_POST['vanish_mode']) ? intval($_POST['vanish_mode']) : 0;

// Validate required fields
if (empty($sender) || empty($receiver) || empty($chat_id)) {
    echo json_encode(["status" => 0, "error" => "Missing required fields"]);
    exit;
}

// Check for duplicate message (same sender, receiver, chat_id, text, and timestamp within 2 seconds)
$checkTimestamp = (time() * 1000) - 2000; // 2 seconds ago
$checkStmt = $conn->prepare("SELECT message_id FROM messages WHERE chat_id = ? AND sender_id = ? AND receiver_id = ? AND text = ? AND timestamp > ? LIMIT 1");
$checkStmt->bind_param("ssssi", $chat_id, $sender, $receiver, $text, $checkTimestamp);
$checkStmt->execute();
$checkResult = $checkStmt->get_result();

if ($checkResult->num_rows > 0) {
    // Duplicate detected, return existing message
    $existing = $checkResult->fetch_assoc();
    $existing_id = $existing['message_id'];
    $checkStmt->close();
    
    // Fetch the full existing message
    $fetchStmt = $conn->prepare("SELECT * FROM messages WHERE message_id = ?");
    $fetchStmt->bind_param("s", $existing_id);
    $fetchStmt->execute();
    $fetchResult = $fetchStmt->get_result();
    $existingMsg = $fetchResult->fetch_assoc();
    $fetchStmt->close();
    
    echo json_encode([
        "status" => 1,
        "message_id" => $existing_id,
        "message" => [
            "message_id" => $existingMsg['message_id'],
            "chat_id" => $existingMsg['chat_id'],
            "sender_id" => $existingMsg['sender_id'],
            "receiver_id" => $existingMsg['receiver_id'],
            "text" => $existingMsg['text'] ?? "",
            "image" => $existingMsg['image'] ?? "",
            "post_id" => $existingMsg['post_id'] ?? "",
            "vanish_mode" => intval($existingMsg['vanish_mode'] ?? 0),
            "seen" => intval($existingMsg['seen'] ?? 0),
            "deleted" => intval($existingMsg['deleted'] ?? 0),
            "edited" => intval($existingMsg['edited'] ?? 0),
            "timestamp" => intval($existingMsg['timestamp'] ?? 0)
        ]
    ]);
    $conn->close();
    exit;
}
$checkStmt->close();

$message_id = uniqid("msg_", true);
$timestamp = time() * 1000;

// Conditionally build query based on whether post_id is provided (like original)
if (!empty($post_id)) {
    $stmt = $conn->prepare("INSERT INTO messages
    (message_id, chat_id, sender_id, receiver_id, text, image, post_id, vanish_mode, seen, deleted, edited, timestamp)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?)");
    $stmt->bind_param("sssssssii", 
        $message_id, $chat_id, $sender, $receiver, $text, $image, $post_id, $vanish_mode, $timestamp
    );
} else {
    $stmt = $conn->prepare("INSERT INTO messages
    (message_id, chat_id, sender_id, receiver_id, text, image, vanish_mode, seen, deleted, edited, timestamp)
    VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?)");
    $stmt->bind_param("ssssssii", 
        $message_id, $chat_id, $sender, $receiver, $text, $image, $vanish_mode, $timestamp
    );
}

if ($stmt->execute()) {
    // Return FULL message object so Android can sync properly - CRITICAL for cross-device sync
    echo json_encode([
        "status" => 1, 
        "message_id" => $message_id,
        "message" => [
            "message_id" => $message_id,
            "chat_id" => $chat_id,
            "sender_id" => $sender,
            "receiver_id" => $receiver,
            "text" => $text,
            "image" => $image,
            "post_id" => $post_id,
            "vanish_mode" => $vanish_mode,
            "seen" => 0,
            "deleted" => 0,
            "edited" => 0,
            "timestamp" => $timestamp
        ]
    ]);
} else {
    echo json_encode(["status" => 0, "error" => $stmt->error]);
}

$stmt->close();
$conn->close();
?>
