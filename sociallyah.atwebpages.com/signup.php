<?php
include 'conn.php';

$response = array();

// UUIDv4 generator
function uuidv4() {
    $data = random_bytes(16);
    $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
    $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
    return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
}

if (isset($_POST['username'], $_POST['email'], $_POST['password'], $_POST['image'], $_POST['bio'])) {

    $username = $_POST['username'];
    $email = $_POST['email'];
    $password = $_POST['password'];
    $image = $_POST['image']; // base64 dp
    $image = mysqli_real_escape_string($conn, $image);
    $bio = $_POST['bio'];
    $online = 0;

    // Generate UUID for uid column
    $uid = uuidv4();

    // Hash password
    $hashed = password_hash($password, PASSWORD_BCRYPT);

    // Check if email exists
    $check = "SELECT id FROM users WHERE email='$email'";
    $result = mysqli_query($conn, $check);

    if (mysqli_num_rows($result) > 0) {
        $response['status'] = 0;
        $response['message'] = "Email already registered";
        echo json_encode($response);
        exit;
    }
       
    // Insert record 
    

    $sql = "INSERT INTO users (uid, username, email, password, bio, dp, online)
            VALUES ('$uid', '$username', '$email', '$hashed', '$bio', '$image', '$online')";

    if (mysqli_query($conn, $sql)) {
        $response['status'] = 1;
        $response['message'] = "Signup successful";
        $response['uid'] = $uid;
        $response['id'] = mysqli_insert_id($conn);
    } else {
        $response['status'] = 0;
        $response['message'] = "Signup failed: " . mysqli_error($conn);
    }

} else {
    $response['status'] = 0;
    $response['message'] = "Required fields are missing";
}

echo json_encode($response);
?>
