<?php
include "conn.php";

header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$chat_id = $_POST['chat_id'] ?? '';
$caller_id = $_POST['caller_id'] ?? '';
$callee_id = $_POST['callee_id'] ?? '';
$call_type = $_POST['call_type'] ?? 'audio';

if (empty($chat_id) || empty($caller_id) || empty($callee_id)) {
    echo json_encode(["status" => 0, "error" => "Missing required fields"]);
    exit;
}

// Check if there's already an active call for this chat
$checkStmt = $conn->prepare("SELECT call_id FROM calls WHERE chat_id = ? AND status IN ('ringing', 'ongoing')");
$checkStmt->bind_param("s", $chat_id);
$checkStmt->execute();
$result = $checkStmt->get_result();

if ($result->num_rows > 0) {
    echo json_encode(["status" => 0, "error" => "Call already exists"]);
    $checkStmt->close();
    $conn->close();
    exit;
}
$checkStmt->close();

// Create new call
$call_id = uniqid("call_", true);
$timestamp = time() * 1000;

$stmt = $conn->prepare("INSERT INTO calls 
    (call_id, chat_id, caller_id, callee_id, call_type, status, timestamp)
    VALUES (?, ?, ?, ?, ?, 'ringing', ?)");

$stmt->bind_param("sssssi", $call_id, $chat_id, $caller_id, $callee_id, $call_type, $timestamp);

if ($stmt->execute()) {
    echo json_encode([
        "status" => 1,
        "call_id" => $call_id,
        "message" => "Call created successfully"
    ]);
} else {
    echo json_encode(["status" => 0, "error" => $stmt->error]);
}

$stmt->close();
$conn->close();
?>

