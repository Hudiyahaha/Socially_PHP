<?php
include "conn.php";

header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$chat_id = $_POST['chat_id'] ?? '';
$user_id = $_POST['user_id'] ?? '';

if (empty($chat_id)) {
    echo json_encode(["status" => 0, "error" => "Missing chat_id"]);
    exit;
}

// Get the most recent call for this chat
$stmt = $conn->prepare("SELECT * FROM calls WHERE chat_id = ? ORDER BY timestamp DESC LIMIT 1");
$stmt->bind_param("s", $chat_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $row = $result->fetch_assoc();
    echo json_encode([
        "status" => 1,
        "call" => [
            "call_id" => $row['call_id'],
            "chat_id" => $row['chat_id'],
            "caller_id" => $row['caller_id'],
            "callee_id" => $row['callee_id'],
            "call_type" => $row['call_type'],
            "status" => $row['status'],
            "timestamp" => intval($row['timestamp'])
        ]
    ]);
} else {
    echo json_encode([
        "status" => 1,
        "call" => null
    ]);
}

$stmt->close();
$conn->close();
?>

