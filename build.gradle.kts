/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

tasks.create("hello") {
    group = "testing"

    doFirst {
        println("Hello world!")
    }
}
