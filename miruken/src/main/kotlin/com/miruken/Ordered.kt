package com.miruken

interface Ordered { var order: Int? }

object OrderedComparator : Comparator<Ordered> {
    override fun compare(o1: Ordered?, o2: Ordered?): Int {
        return when {
            o1 == o2 -> 0
            o2?.order == null -> -1
            o1?.order == null -> 1
            else -> o1.order!! - o2.order!!
        }
    }
}