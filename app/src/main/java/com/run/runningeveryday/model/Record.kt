package com.run.runningeveryday.model

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


    fun getGradeStandard(sex: String, age: Long, distance: Int): ArrayList<StandardRecord> {

        val standardList = ArrayList<StandardRecord>()

        when(distance) {

            1500 -> {

                when(sex) {

                    "남" -> {

                        when (age) {

                            in 0..25 -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0 ,368, 1))
                                    standardList.add(StandardRecord(369, 378, 2))
                                    standardList.add(StandardRecord(379, 388, 3))
                                }
                            }

                            else -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 378, 1))
                                    standardList.add(StandardRecord(379, 388, 2))
                                    standardList.add(StandardRecord(389, 398, 3))
                                }
                            }
                        }
                    }

                    "여" -> {

                        when (age) {

                            in 0..25 -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 459, 1))
                                    standardList.add(StandardRecord(460, 469, 2))
                                    standardList.add(StandardRecord(470, 479, 3))
                                }
                            }

                            else -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 469, 1))
                                    standardList.add(StandardRecord(470, 479, 2))
                                    standardList.add(StandardRecord(480, 489, 3))
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

                                standardList.apply {
                                    standardList.add(StandardRecord(0 ,765, 0))
                                    standardList.add(StandardRecord(766, 832, 1))
                                    standardList.add(StandardRecord(833, 899, 2))
                                    standardList.add(StandardRecord(900, 966, 3))
                                }
                            }

                            in 31..40 -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 795, 0))
                                    standardList.add(StandardRecord(796, 872, 1))
                                    standardList.add(StandardRecord(873, 949, 2))
                                    standardList.add(StandardRecord(950, 1026, 3))
                                }
                            }

                            in 41..50 -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 825, 0))
                                    standardList.add(StandardRecord(826, 905, 1))
                                    standardList.add(StandardRecord(906, 986, 2))
                                    standardList.add(StandardRecord(987, 1066, 3))
                                }
                            }

                            else -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 885, 0))
                                    standardList.add(StandardRecord(886, 979, 1))
                                    standardList.add(StandardRecord(980, 1072, 2))
                                    standardList.add(StandardRecord(1073, 1166, 3))
                                }
                            }

                        }
                    }

                    "여" -> {

                        when(age) {

                            in 0..30 -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 918, 0))
                                    standardList.add(StandardRecord(919, 998, 1))
                                    standardList.add(StandardRecord(999, 1079, 2))
                                    standardList.add(StandardRecord(1080, 1159, 3))
                                }
                            }

                            in 31..40 -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 954, 0))
                                    standardList.add(StandardRecord(955, 1046, 1))
                                    standardList.add(StandardRecord(1047, 1139, 2))
                                    standardList.add(StandardRecord(1140, 1231, 3))
                                }
                            }

                            in 41..50 -> {

                                standardList.apply {
                                    standardList.add(StandardRecord(0, 990, 0))
                                    standardList.add(StandardRecord(991, 1086, 1))
                                    standardList.add(StandardRecord(1087, 1183, 2))
                                    standardList.add(StandardRecord(1184, 1279, 3))
                                }
                            }

                            else -> {
                                standardList.apply {
                                    standardList.add(StandardRecord(0, 1062, 0))
                                    standardList.add(StandardRecord(1063, 1174, 1))
                                    standardList.add(StandardRecord(1175, 1287, 2))
                                    standardList.add(StandardRecord(1288, 1399, 3))
                                }
                            }
                        }
                    }
                }
            }
        }
        return standardList
    }
}

data class StandardRecord(
    val startRange: Int,
    val endRange: Int,
    val grade: Int
)



