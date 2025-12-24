<?php
include 'conn.php';

$response = array();

if (isset($_POST['email'], $_POST['password'])) {

    $email = $_POST['email'];
    $password = $_POST['password'];

    $sql = "SELECT * FROM users WHERE email='$email' LIMIT 1";
    $result = mysqli_query($conn, $sql);

    if (mysqli_num_rows($result) == 1) {

        $row = mysqli_fetch_assoc($result);
        $storedHash = $row['password'];

        if (password_verify($password, $storedHash)) {

            // 🔥 Mark user online
            $uid = $row['id']; 
            $update = "UPDATE users SET online = 1 WHERE id = '$uid'";
            mysqli_query($conn, $update);

            // Return updated online state
            $row['online'] = 1;

            $response['status'] = 1;
            $response['message'] = "Login successful";

            $response['id'] = $row['id'];
            $response['uid'] = $row['uid'];
            $response['username'] = $row['username'];
            $response['email'] = $row['email'];
            $response['dp'] = $row['dp'];
            $response['bio'] = $row['bio'];
            $response['online'] = $row['online'];

        } else {
            $response['status'] = 0;
            $response['message'] = "Incorrect password";
        }

    } else {
        $response['status'] = 0;
        $response['message'] = "Email not found";
    }

} else {
    $response['status'] = 0;
    $response['message'] = "Required fields missing";
}

echo json_encode($response);
?>
