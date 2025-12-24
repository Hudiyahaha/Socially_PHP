<?php
include "conn.php";

$follower = $_POST['follower_id'];
$following = $_POST['following_id'];

$stmt = $conn->prepare("SELECT status FROM follows WHERE follower_id=? AND following_id=?");
$stmt->bind_param("ss", $follower, $following);
$stmt->execute();
$res = $stmt->get_result();

if ($row = $res->fetch_assoc()) {
    if ($row['status'] == 'requested') {
        echo json_encode(["status"=>0, "message"=>"Already requested"]);
        exit;
    }
    if ($row['status'] == 'accepted') {
        echo json_encode(["status"=>0, "message"=>"Already following"]);
        exit;
    }
}

$stmt = $conn->prepare("INSERT INTO follows (follower_id, following_id, status) VALUES (?, ?, 'requested')");
$stmt->bind_param("ss", $follower, $following);

if ($stmt->execute()) {
    echo json_encode(["status"=>1, "message"=>"Follow request sent"]);
} else {
    echo json_encode(["status"=>0, "message"=>"Error"]);
}
?>
