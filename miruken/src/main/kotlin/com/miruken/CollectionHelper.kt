package com.miruken

fun <T: Comparable<T>> MutableList<T>.addSorted(
        element: T
) {
    if (isEmpty()) {
        add(element)
    } else if (this[lastIndex] <= element) {
        add(element)
        return
    } else if (this[0] >= element) {
        add(0, element)
        return
    } else {
        val index = binarySearch(element)
        if (index < 0) {
            add(-(index + 1), element)
        } else {
            add(index, element)
        }
    }
}

fun <T> MutableList<T>.addSorted(
        element:    T,
        comparator: Comparator<in T>) {
    if (isEmpty()) {
        add(element)
    } else if (comparator.compare(this[lastIndex], element) <= 0) {
        add(element)
        return
    } else if (comparator.compare(this[0], element) >= 0) {
        add(0, element)
        return
    } else {
        val index = binarySearch(element, comparator)
        if (index < 0) {
            add(-(index + 1), element)
        } else {
            add(index, element)
        }
    }
}