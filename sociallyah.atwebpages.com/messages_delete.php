<?php
include "conn.php";

$id = $_POST['message_id'];

$q = "SELECT timestamp FROM messages WHERE message_id=?";
$stmt = $conn->prepare($q);
$stmt->bind_param("s", $id);
$stmt->execute();
$res = $stmt->get_result()->fetch_assoc();

$now = time() * 1000;
$diff = ($now - $res['timestamp']) / 1000;

if ($diff > 300) {
    echo json_encode(["status"=>0,"msg"=>"Delete window expired"]);
    exit;
}

$u = $conn->prepare("UPDATE messages SET deleted=1 WHERE message_id=?");
$u->bind_param("s", $id);
$u->execute();

echo json_encode(["status"=>1]);
?>
