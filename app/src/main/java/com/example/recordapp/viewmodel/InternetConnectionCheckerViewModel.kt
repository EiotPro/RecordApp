package com.example.recordapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.recordapp.network.InternetConnectionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InternetConnectionCheckerViewModel @Inject constructor(
    val connectionChecker: InternetConnectionChecker
) : ViewModel() 