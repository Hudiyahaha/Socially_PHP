package com.wethrive.i23_0761_i23_0765

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {


    @Test
    fun testPostAppearsAfterLogin() {
        val auth = FirebaseAuth.getInstance()
        val task = auth.signInWithEmailAndPassword("aleezazhar@gmail.com", "aleezazhar")
        Tasks.await(task)

        if (task.isSuccessful) {
            ActivityScenario.launch(MainActivity5::class.java)
            Thread.sleep(7000)
            onView(withId(R.id.postRecycler)).check(matches(isDisplayed()))
        } else {
            throw AssertionError("Firebase login failed: ${task.exception?.message}")
        }
    }
    @Test
    fun testStoryAppearsAfterLogin() {
        val auth = FirebaseAuth.getInstance()
        val task = auth.signInWithEmailAndPassword("aleezazhar@gmail.com", "aleezazhar")
        Tasks.await(task)

        if (task.isSuccessful) {
            ActivityScenario.launch(MainActivity5::class.java)
            Thread.sleep(7000)
            onView(withId(R.id.storyRecyclerView)).check(matches(isDisplayed()))
        } else {
            throw AssertionError("Firebase login failed: ${task.exception?.message}")
        }
    }
}