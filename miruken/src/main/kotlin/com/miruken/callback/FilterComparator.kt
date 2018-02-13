package com.miruken.callback

object FilterComparator : Comparator<Filtering<*,*>> {
    override fun compare(
            o1: Filtering<*, *>?,
            o2: Filtering<*, *>?
    ): Int {
        return when{
            o1 == o2 -> 0
            o1?.order == null -> -1
            o2?.order == null -> 1
            else -> o1.order!! - o2.order!!
        }
    }
}