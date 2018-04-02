package com.miruken.validate

import java.time.LocalDate

open class Model : ValidationAware {
    override var validationOutcome: ValidationResult.Outcome? = null
}

open class Person : Model() {
    var firstName: String? = null
    var lastName:  String? = null
}

class Coach : Person() {
    var license: String? = null
}

class Player : Person() {
    var dob: LocalDate? = null
}

class Team : Model() {
    var name:     String?       = null
    var division: String?       = null
    var coach:    Coach?        = null
    var players:  List<Player>? = null
    var registed: Boolean       = false
}

