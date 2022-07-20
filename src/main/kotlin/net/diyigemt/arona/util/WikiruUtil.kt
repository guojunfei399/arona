package net.diyigemt.arona.util

import net.diyigemt.arona.entity.Activity
import net.diyigemt.arona.util.ScriptInterpreter.color
import net.diyigemt.arona.util.ScriptInterpreter.default
import net.diyigemt.arona.util.ScriptInterpreter.ref
import java.text.SimpleDateFormat
import java.util.*

/**
 *@Author Haythem723
 *@Create 2022/7/9
 */

object WikiruUtil {
  private const val katakanaRegex = """_[ァ-ヶー]+"""
  fun getValidData(raw : String) : String{
    val start = "&size(16){''報酬受け取り期間''};"
    var tmp = raw.substring(raw.indexOf("&size(16){''開催中のイベント''};"), raw.indexOf("#region([[イベント]]一覧)"))
    tmp = tmp.removeRange(tmp.indexOf(start) + start.length, tmp.indexOf("&size(16){''開催予定のイベント''};"))
    val regex = """//.*\n"""
    val tag = Regex(regex).find(tmp)
    if(tag != null){
      tmp = tmp.substring(0, tag.range.first) + tmp.substring(tag.range.last)
    }

    return tmp
  }

  fun analyze(code : String) : Pair<MutableList<Activity>, MutableList<Activity>>{
    val active : MutableList<Activity> = mutableListOf()
    val pending : MutableList<Activity> = mutableListOf()
    var pointer = 0
    while (pointer != -1){
      val name = scriptDecoder(code.substring(code.indexOf("-", pointer), code.indexOf("\n", code.indexOf("-", pointer) + 2)))
      val katakanaName = Regex(katakanaRegex).findAll(name)
      var test = Regex(katakanaRegex).find(name)?.value
      if(test == null){
        test = ""
      }
      val res = katakanaName.toList()
//      for(i in res){
//        print(i.value + "\n")
//      }
      pointer =code.indexOf("\n", pointer)
      val time = code.substring(code.indexOf("(", pointer) + 1, code.indexOf(")", pointer)).replace("メンテナンス後", ActivityUtil.ServerMaintenanceEndTimeJP)
      val timeStart = SimpleDateFormat("yyyy/M/d HH:mm").parse(time)
      val timeEnd = SimpleDateFormat("M/d HH:mm").parse(time.substring(time.indexOf("～") + 1))
      timeEnd.year = Calendar.getInstance().get(Calendar.YEAR) - 1900

      ActivityUtil.doInsert(Calendar.getInstance().time, timeStart, timeEnd, active, pending, name, test)
      pointer = code.indexOf(")\n", pointer)
      pointer = code.indexOf("-", pointer)
    }
    active.sortByDescending { it.type.level }
    pending.sortByDescending { it.type.level }

    return active to pending
  }

  private fun scriptDecoder(code : String) : String{
    val res: String
    val regex = """&ref\("""
//    val nameRegex = """[ァ-ヶぁ-ゞァ-ヶー-龠。-゜+^ -~。-゜\p{P}]+"""
    val tag = Regex(regex).find(code)
    res = if (tag != null){
      Tags.getInterpreterByTag(tag.value.substring(1, tag.value.length - 1))(code.substring(code.indexOf("("), code.indexOf(";")))
    }
    else{
//      res = Regex(nameRegex).find(code)?.value.toString()
//      res = code.substring(code.indexOf("[[") + 2, code.indexOf(">"))
      if(code.indexOf(">") != -1){
        code.substring(code.indexOf("-[[") + 3, code.indexOf(">"))
      }
      else code.substring(code.indexOf("-[[") + 3, code.indexOf("]]"))
    }
    return scriptInterpreter(res)
  }

  private fun scriptInterpreter(code : String) : String{
    var output = code
    val regex = """&[A-z]*\([A-z\d]*\)\{.*};"""
    val res = Regex(regex).findAll(code).toList()
    for(i in res){
      val tmp = Tags.getInterpreterByTag(i.value.substring(1, i.value.indexOf("(")))(i.value.substring(i.value.indexOf("("), i.value.indexOf(";")))
      output = output.replace(i.value, tmp)
    }

    return output
  }
}

private enum class Tags(val Callback : (String) -> String){
  DEFAULT(::default),
  REF(::ref),
  COLOR(::color);

  companion object{
    fun getInterpreterByTag(tag : String) : ((String) -> String) {
      for(i in values().iterator()){
        if(i.name.lowercase() == tag) return i.Callback
      }

      return DEFAULT.Callback
    }
  }
}

private object ScriptInterpreter{
  fun default(code : String) : String = code

  fun ref (code : String) : String = code.substring(code.lastIndexOf("/") + 1, code.indexOf("."))

  fun color (code: String) : String {
    return code.substring(code.lastIndexOf("{") + 1, code.lastIndexOf("}"))
  }
}