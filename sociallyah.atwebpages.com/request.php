<?php
include "conn.php";

//current user id
$following = $_POST['following_id'];

$stmt = $conn->prepare("
    SELECT follower_id 
    FROM follows
    WHERE following_id = ? AND status = 'requested'
");
$stmt->bind_param("s", $following);
$stmt->execute();
$result = $stmt->get_result();

$requests = [];

while ($row = $result->fetch_assoc()) {
    $requests[] = ["follower_id" => $row["follower_id"]];
}

echo json_encode([
    "status" => 1,
    "count" => count($requests),
    "requests" => $requests
]);
?>
