<?php
include "conn.php";

header('Content-Type: application/json');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

$chat_id = $_POST['chat_id'] ?? '';
$by = $_POST['by'] ?? '';
$to = $_POST['to'] ?? '';

if (empty($chat_id) || empty($by) || empty($to)) {
    echo json_encode(["status" => 0, "error" => "Missing required fields"]);
    exit;
}

$timestamp = time() * 1000;

// Check if a screenshot was already recorded recently (within last 3 seconds) to prevent duplicates
$checkStmt = $conn->prepare("SELECT screenshot_id FROM screenshots WHERE chat_id = ? AND by_user = ? AND to_user = ? AND timestamp > ?");
$recentTimestamp = $timestamp - 3000; // 3 seconds ago
$checkStmt->bind_param("sssi", $chat_id, $by, $to, $recentTimestamp);
$checkStmt->execute();
$checkResult = $checkStmt->get_result();

if ($checkResult->num_rows > 0) {
    // Duplicate detected, return existing record
    $existing = $checkResult->fetch_assoc();
    $checkStmt->close();
    echo json_encode([
        "status" => 1,
        "screenshot_id" => $existing['screenshot_id'],
        "message" => "Screenshot event already recorded (duplicate prevented)"
    ]);
    $conn->close();
    exit;
}
$checkStmt->close();

$screenshot_id = uniqid("shot_", true);

// Insert screenshot event
$stmt = $conn->prepare("INSERT INTO screenshots (screenshot_id, chat_id, by_user, to_user, timestamp) VALUES (?, ?, ?, ?, ?)");
$stmt->bind_param("ssssi", $screenshot_id, $chat_id, $by, $to, $timestamp);

if ($stmt->execute()) {
    echo json_encode([
        "status" => 1,
        "screenshot_id" => $screenshot_id,
        "message" => "Screenshot event recorded"
    ]);
} else {
    echo json_encode(["status" => 0, "error" => $stmt->error]);
}

$stmt->close();
$conn->close();
?>

