package com.miruken.validate

import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

open class Model : ValidationAware {
    override var validationOutcome: ValidationResult.Outcome? = null
}

open class Person : Model() {
    @NotEmpty
    var firstName: String? = null
    @NotEmpty
    var lastName:  String? = null
}

class Coach : Person() {
    @NotEmpty
    var license: String? = null
}

class Player : Person() {
    @NotNull
    var dob: LocalDate? = null
}

class Team : Model() {
    var id:       Int? = null
    var active:   Boolean? = null
    @NotEmpty
    var name:     String? = null
    @Pattern(regexp  = "^[u|U]\\d\\d?\$",
             message = "division must match U followed by age")
    var division: String? = null
    @NotNull @Valid
    var coach:    Coach? = null
    @Valid
    var players:  List<Player>? = null
    var registed: Boolean? = null
}

