package com.example.runningeveryday

class Record {

    fun timeFormat(time: Long) : String {
        return String.format("%02d : %02d", time / 60, time % 60)
    }

    fun getGrade(sex: String, age: Long, distance: Int, time: Long) : Int {

        var grade = -1

        when(distance) {

            1500 -> {

                when(sex) {

                    "남" -> {

                        when (age) {

                            in 0..25 -> {

                                grade = when (time) {

                                    // 26~30
                                    in 0..368 -> 1
                                    in 369..378 -> 2
                                    in 379..388 -> 3
                                    else -> 4
                                }
                            }

                            else -> {

                                grade = when (time) {

                                    // 36~40
                                    in 0..378 -> 1
                                    in 379..388 -> 2
                                    in 389..398 -> 3
                                    else -> 4
                                }
                            }
                        }
                    }

                    "여" -> {

                        when (age) {

                            in 0..25 -> {

                                grade = when (time) {

                                    // 26~30
                                    in 0..459 -> 1
                                    in 460..469 -> 2
                                    in 470..479 -> 3
                                    else -> 4
                                }
                            }

                            else -> {

                                grade = when (time) {

                                    // 36~40
                                    in 0..469 -> 0
                                    in 470..479 -> 1
                                    in 480..489 -> 2
                                    else -> 4
                                }
                            }
                        }
                    }
                }
            }

            3000 -> {

                when(sex) {

                    "남" -> {

                        when(age) {

                            in 0..30 -> {

                                grade = when(time) {

                                    // 26~30
                                    in 0..765 -> 0
                                    in 766..832 -> 1
                                    in 833..899 -> 2
                                    in 900..966 -> 3
                                    else -> 4
                                }
                            }

                            in 31..40 -> {

                                grade = when(time) {

                                    // 36~40
                                    in 0..795 -> 0
                                    in 796..872 -> 1
                                    in 873..949 -> 2
                                    in 950..1026 -> 3
                                    else -> 4
                                }
                            }

                            in 41..50 -> {

                                grade = when(time) {

                                    // 44~46
                                    in 0..825 -> 0
                                    in 826..905 -> 1
                                    in 906..986 -> 2
                                    in 987..1066 -> 3
                                    else -> 4
                                }
                            }

                            else -> {
                                grade = when(time) {

                                    // 54~55
                                    in 0..885 -> 0
                                    in 886..979 -> 1
                                    in 980..1072 -> 2
                                    in 1073..1166 -> 3
                                    else -> 4
                                }
                            }

                        }
                    }

                    "여" -> {

                        when(age) {

                            in 0..30 -> {

                                grade = when(time) {

                                    // 26~30
                                    in 0..918 -> 0
                                    in 919..998 -> 1
                                    in 999..1079 -> 2
                                    in 1080..1159 -> 3
                                    else -> 4
                                }
                            }

                            in 31..40 -> {

                                grade = when(time) {

                                    // 36~40
                                    in 0..954 -> 0
                                    in 955..1046 -> 1
                                    in 1047..1139 -> 2
                                    in 1140..1231 -> 3
                                    else -> 4
                                }
                            }

                            in 41..50 -> {

                                grade = when(time) {

                                    // 44~46
                                    in 0..990 -> 0
                                    in 991..1086 -> 1
                                    in 1087..1183 -> 2
                                    in 1184..1279 -> 3
                                    else -> 4
                                }
                            }

                            else -> {
                                grade = when(time) {

                                    // 54~55
                                    in 0..1062 -> 0
                                    in 1063..1174 -> 1
                                    in 1175..1287 -> 2
                                    in 1288..1399 -> 3
                                    else -> 4
                                }
                            }

                        }
                    }

                }
            }
        }
        return grade
    }
}



