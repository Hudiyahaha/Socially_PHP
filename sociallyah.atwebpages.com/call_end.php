<?php
include "conn.php";

header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$call_id = $_POST['call_id'] ?? '';
$chat_id = $_POST['chat_id'] ?? '';

if (empty($call_id) && empty($chat_id)) {
    echo json_encode(["status" => 0, "error" => "Missing call_id or chat_id"]);
    exit;
}

if (!empty($call_id)) {
    $stmt = $conn->prepare("UPDATE calls SET status = 'ended' WHERE call_id = ?");
    $stmt->bind_param("s", $call_id);
} else {
    $stmt = $conn->prepare("UPDATE calls SET status = 'ended' WHERE chat_id = ? AND status IN ('ringing', 'ongoing')");
    $stmt->bind_param("s", $chat_id);
}

if ($stmt->execute()) {
    echo json_encode([
        "status" => 1,
        "message" => "Call ended",
        "affected_rows" => $stmt->affected_rows
    ]);
} else {
    echo json_encode(["status" => 0, "error" => $stmt->error]);
}

$stmt->close();
$conn->close();
?>

