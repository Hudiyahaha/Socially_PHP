<?php
include "conn.php";

header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$call_id = $_POST['call_id'] ?? '';
$status = $_POST['status'] ?? '';

if (empty($call_id) || empty($status)) {
    echo json_encode(["status" => 0, "error" => "Missing required fields"]);
    exit;
}

// Validate status
$validStatuses = ['ringing', 'ongoing', 'ended', 'declined'];
if (!in_array($status, $validStatuses)) {
    echo json_encode(["status" => 0, "error" => "Invalid status"]);
    exit;
}

$stmt = $conn->prepare("UPDATE calls SET status = ? WHERE call_id = ?");
$stmt->bind_param("ss", $status, $call_id);

if ($stmt->execute()) {
    $affected = $stmt->affected_rows;
    echo json_encode([
        "status" => 1,
        "message" => "Call updated",
        "affected_rows" => $affected
    ]);
} else {
    echo json_encode(["status" => 0, "error" => $stmt->error]);
}

$stmt->close();
$conn->close();
?>

