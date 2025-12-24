<?php
require_once "conn.php";

$userId = $_POST["userId"] ?? "";
$status = $_POST["status"] ?? "";

if(empty($userId) || $status == ""){
    echo "Missing params";
    exit;
}

$sql = "UPDATE users SET online='$status' WHERE id='$userId'";
if(mysqli_query($conn, $sql)){
    echo "ok";
} else {
    echo "error";
}
?>
