package me.earzuchan.markdo.duties

import kotlinx.serialization.Serializable


@Serializable
sealed class AppNavis {
    @Serializable
    data object Main : AppNavis()

    @Serializable
    data object Login : AppNavis()

    @Serializable
    data object Splash : AppNavis()
}

@Serializable
sealed class MainNavis {
    @Serializable
    data object Dashboard : MainNavis()

    @Serializable
    data object Course : MainNavis()

    @Serializable
    data object My : MainNavis()
}

@Serializable
sealed class CourseNavis {
    @Serializable
    data object AllCourses : CourseNavis()

    @Serializable
    data class CourseDetail(val courseId: Int) : CourseNavis()
}

@Serializable
sealed class MyNavis {
    @Serializable
    data object Overview : MyNavis()

    @Serializable
    data object TransMan : MyNavis()

    @Serializable
    data object Settings : MyNavis()

    @Serializable
    data object Grades : MyNavis()
}
