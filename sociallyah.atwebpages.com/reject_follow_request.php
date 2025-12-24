<?php
include "conn.php";

$follower = $_POST['follower_id'];
$following = $_POST['following_id'];

$stmt = $conn->prepare("
    DELETE FROM follows 
    WHERE follower_id=? AND following_id=? AND status='requested'
");
$stmt->bind_param("ss", $follower, $following);
$stmt->execute();

if ($stmt->affected_rows > 0) {
    echo json_encode(["status"=>1, "message"=>"Request rejected"]);
} else {
    echo json_encode(["status"=>0, "message"=>"No pending request found"]);
}
?>
