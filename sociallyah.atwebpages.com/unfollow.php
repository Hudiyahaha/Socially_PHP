<?php
include "conn.php";

$follower = $_POST['follower_id'];
$following = $_POST['following_id'];

$stmt = $conn->prepare("DELETE FROM follows WHERE follower_id=? AND following_id=? AND status='accepted'");
$stmt->bind_param("ss", $follower, $following);
$stmt->execute();

echo json_encode(["status"=>1, "message"=>"Unfollowed"]);
?>
