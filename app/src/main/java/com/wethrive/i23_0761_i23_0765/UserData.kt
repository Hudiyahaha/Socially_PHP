package com.wethrive.i23_0761_i23_0765

data class  UserData(var id:String?=null,val uname:String,var email:String,var dp:String, var bio:String, val online: Int = 0){
    constructor():this("","","","","Socially App",0)
}
