package me.simpleHook.fragment

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import me.simpleHook.R


open class BaseFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    fun back() {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val inputMethodManage =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManage.hideSoftInputFromWindow(
            requireActivity().findViewById<FragmentContainerView>(
                R.id.fragment
            ).windowToken, 0
        )
        navController.navigateUp()
    }
}