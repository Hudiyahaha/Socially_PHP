<?php
include "conn.php";

$follower = $_POST['follower_id'];
$following = $_POST['following_id'];

// Only accept if there is a requested row
$stmt = $conn->prepare("
    UPDATE follows 
    SET status='accepted' 
    WHERE follower_id=? AND following_id=? AND status='requested'
");
$stmt->bind_param("ss", $follower, $following);

if ($stmt->execute() && $stmt->affected_rows > 0) {
    echo json_encode(["status"=>1, "message"=>"Request accepted"]);
} else {
    echo json_encode(["status"=>0, "message"=>"No pending request found"]);
}
?>
