<?php
header('Content-Type: application/json');
require_once "conn.php";

//THIS API TAKES userID and responds with more than just dp.

// Accept userId from POST or GET
$userId = $_POST['userId'] ?? '';

if (empty($userId)) {
    echo json_encode(['status' => 0, 'message' => 'User ID is required']);
    exit;
}

// Fetch display picture from users table where uid matches userId
$stmt = $conn->prepare("SELECT uid, username, dp, bio FROM users WHERE uid = ? LIMIT 1");
$stmt->bind_param("s", $userId);

if ($stmt->execute()) {
    $res = $stmt->get_result();
    
    if ($res->num_rows > 0) {
        $row = $res->fetch_assoc();
        $dp = $row['dp'] ?? '';
        
        // Clean up dp string (remove whitespace and newlines)
        if (!empty($dp)) {
            $dp = trim($dp);
            $dp = str_replace(array("\r", "\n", " "), "", $dp);
        }
        
        // Get bio value, handle NULL case
        $bio = isset($row['bio']) ? $row['bio'] : '';
        if ($bio === null) {
            $bio = "Hey there! I am using socially.";
        }
        
        echo json_encode([
            'status' => 1,
            'message' => 'Display picture fetched successfully',
            'uid' => $row['uid'] ?? '',
            'username' => $row['username'] ?? '',
            'bio' => $bio,
            'dp' => $dp
        ]);
    } else {
        echo json_encode([
            'status' => 0,
            'message' => 'User not found'
        ]);
    }
} else {
    echo json_encode([
        'status' => 0,
        'message' => 'Query failed'
    ]);
}

$stmt->close();
$conn->close();
?>

