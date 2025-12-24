<?php
include "conn.php";

$follower = $_POST['follower_id'];
$following = $_POST['following_id'];

$stmt = $conn->prepare("SELECT status FROM follows WHERE follower_id=? AND following_id=?");
$stmt->bind_param("ss", $follower, $following);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    if ($row['status'] == 'accepted') {
        echo json_encode(["status"=>1, "state"=>"Following"]);
        exit;
    } else if ($row['status'] == 'requested') {
        echo json_encode(["status"=>1, "state"=>"Requested"]);
        exit;
    }
}

echo json_encode(["status"=>1, "state"=>"Follow"]);
?>
