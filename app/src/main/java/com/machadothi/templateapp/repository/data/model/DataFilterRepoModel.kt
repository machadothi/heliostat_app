package com.machadothi.templateapp.repository.data.model

sealed class DataFilterRepoModel {

    data object Today : DataFilterRepoModel()
    data class Custom(
        val filter: String
    ) : DataFilterRepoModel()
}