package com.wethrive.i23_0761_i23_0765

data class UserData(var id:String?=null,val name:String,var email:String,var password:String){
    constructor():this("","","","")
}
