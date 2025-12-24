<?php
header('Content-Type: application/json');
require_once "conn.php";

$userId = $_POST['userId'] ?? '';
$type = $_POST['type'] ?? 'all';  // all | followers | following

if (empty($userId)) {
    echo json_encode(['status' => 0, 'message' => 'User ID is required']);
    exit;
}

if ($type === "followers") {

    // USERS WHO FOLLOW ME
    $sql = "
        SELECT u.uid, u.username, u.dp, u.online
        FROM follows f 
        JOIN users u ON u.uid = f.follower_id
        WHERE f.following_id = ? AND f.status = 'accepted'
    ";

} else if ($type === "following") {

    // USERS I FOLLOW
    $sql = "
        SELECT u.uid, u.username, u.dp, u.online
        FROM follows f 
        JOIN users u ON u.uid = f.following_id
        WHERE f.follower_id = ? AND f.status = 'accepted'
    ";

} else {

    // OLD — get all users except me
    $sql = "SELECT uid, username, dp, online FROM users WHERE id != ?";
}

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $userId);

$users = [];

if ($stmt->execute()) {
    $res = $stmt->get_result();

    while ($row = $res->fetch_assoc()) {
        $dp = trim($row['dp'] ?? '');
        $dp = str_replace(array("\r", "\n", " "), "", $dp);

        $users[] = [
            'uid' => $row['uid'],
            'username' => $row['username'],
            'dp' => $dp,
            'online' => intval($row['online'])
        ];
    }

    echo json_encode($users);
} 
else {
    echo json_encode(['status' => 0, 'message' => 'Query failed']);
}

$stmt->close();
$conn->close();
?>
