package com.buildfinance.app

import android.app.*
import android.os.Bundle
import android.content.*
import android.database.sqlite.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FinanceDb(c:Context):SQLiteOpenHelper(c,"finance.db",null,1){
 override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT NOT NULL,category TEXT NOT NULL,project TEXT NOT NULL,note TEXT NOT NULL,amount REAL NOT NULL,created INTEGER NOT NULL)")}
 override fun onUpgrade(db:SQLiteDatabase,a:Int,b:Int){}
}
data class Operation(val id:Long,val kind:String,val category:String,val project:String,val note:String,val amount:Double,val created:Long)
class MainActivity:Activity(){
 private lateinit var db:FinanceDb
 private lateinit var body:LinearLayout
 private var page=0
 private var filter="Все объекты"
 private val navy=Color.rgb(19,32,54)
 private val blue=Color.rgb(56,105,235)
 private val green=Color.rgb(14,148,108)
 private val red=Color.rgb(215,74,91)
 private val muted=Color.rgb(111,126,146)
 private val bg=Color.rgb(249,250,253)
 private val gold=Color.rgb(204,169,113)
 private val money=NumberFormat.getCurrencyInstance(Locale.GERMANY)
 private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
 private fun shape(color:Int,r:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
 private fun col()=LinearLayout(this).apply{orientation=1}
 private fun row()=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
 private fun text(s:String,size:Float=15f,color:Int=navy,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL;if(bold)setTypeface(null,Typeface.BOLD)}
 private fun card()=col().apply{background=shape(Color.WHITE,20);setPadding(dp(17),dp(17),dp(17),dp(17));elevation=dp(1).toFloat()}
 private fun add(p:LinearLayout,v:View,top:Int=0){p.addView(v,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(top)})}
 private fun records():List<Operation>{
  val result=mutableListOf<Operation>()
  db.readableDatabase.rawQuery("SELECT id,kind,category,project,note,amount,created FROM entries ORDER BY created DESC,id DESC",null).use{c->
   while(c.moveToNext())result.add(Operation(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getDouble(5),c.getLong(6)))
  }
  return result
 }
 private fun eur(n:Double)=money.format(n)
 override fun onCreate(state:Bundle?){super.onCreate(state);db=FinanceDb(this);window.statusBarColor=bg;window.navigationBarColor=bg;window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;render()}
 private fun button(label:String,action:()->Unit)=text(label,16f,Color.WHITE,true).apply{gravity=Gravity.CENTER;background=shape(blue,15);setPadding(dp(12),dp(16),dp(12),dp(16));setOnClickListener{action()}}
 private fun title(s:String){add(body,text(s,22f,navy,true),12)}
 private fun section(s:String){add(body,text(s,19f,navy,true),25)}
 private fun sectionLink(s:String,link:String,action:()->Unit){val r=row();r.addView(text(s,19f,navy,true),LinearLayout.LayoutParams(0,-2,1f));r.addView(text(link+"  ›",12f,muted).apply{setOnClickListener{action()}});add(body,r,25)}
 private fun render(){
  val shell=col().apply{setBackgroundColor(bg)}
  setContentView(shell)
  val head=row().apply{setPadding(dp(20),dp(14),dp(20),dp(12))}
  val logo=android.widget.ImageView(this).apply{setImageResource(com.buildfinance.app.R.drawable.ic_brand);scaleType=android.widget.ImageView.ScaleType.FIT_CENTER}
  head.addView(logo,LinearLayout.LayoutParams(dp(39),dp(43)).apply{marginEnd=dp(12)})
  val brand=col()
  brand.addView(text("BUILD FINANCE",20f,navy,true))
  brand.addView(text("Управление финансами",12f,muted))
  head.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
  head.addView(text("⋯",30f,navy,true).apply{gravity=Gravity.CENTER;setOnClickListener{options()}},LinearLayout.LayoutParams(dp(45),dp(45)))
  add(shell,head)
  val scroll=ScrollView(this)
  body=col().apply{setPadding(dp(20),dp(5),dp(20),dp(28))}
  scroll.addView(body)
  shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  when(page){0->dashboard();1->projects();2->operations();else->analytics()}
  val nav=row().apply{setPadding(dp(8),dp(8),dp(8),dp(10));setBackgroundColor(Color.WHITE)}
  val names=arrayOf("⌂\nГлавная","▱\nОбъекты","⇄\nОперации","▥\nОтчёты")
  for(i in names.indices){
   val item=text(names[i],12f,if(i==page)blue else muted,i==page).apply{gravity=Gravity.CENTER;if(i==page)background=shape(Color.rgb(235,241,255),13);setOnClickListener{page=i;render()}}
   nav.addView(item,LinearLayout.LayoutParams(0,dp(58),1f).apply{marginStart=dp(3);marginEnd=dp(3)})
  }
  add(shell,nav)
 }
 private fun dashboard(){
  val list=records()
  val income=list.filter{it.kind=="Доход"}.sumOf{it.amount}
  val spent=list.filter{it.kind!="Доход"}.sumOf{it.amount}
  val hero=col().apply{background=shape(navy,24);setPadding(dp(23),dp(23),dp(23),dp(23))}
  val top=row()
  top.addView(text("ОБЩИЙ БАЛАНС",12f,Color.rgb(183,199,222),true),LinearLayout.LayoutParams(0,-2,1f))
  top.addView(text("▥",21f,gold,true))
  add(hero,top)
  add(hero,text(eur(income-spent),33f,Color.WHITE,true),12)
  add(hero,text("По всем строительным объектам",12f,Color.rgb(183,199,222)),5)
  val line=View(this).apply{setBackgroundColor(Color.rgb(66,79,99))}
  hero.addView(line,LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(20);bottomMargin=dp(15)})
  val stats=row()
  val figures=listOf(Triple("ОБЪЕКТЫ",list.map{it.project}.distinct().size.toString(),Color.WHITE),Triple("ДОХОДЫ",eur(income),Color.rgb(112,221,180)),Triple("РАСХОДЫ",eur(spent),Color.rgb(255,157,163)))
  for((name,value,color) in figures){
   val box=col()
   add(box,text(value,15f,color,true))
   add(box,text(name,10f,Color.rgb(183,199,222)),5)
   stats.addView(box,LinearLayout.LayoutParams(0,-2,1f))
  }
  add(hero,stats)
  add(body,hero,12)
  val actions=row()
  val actionNames=listOf(Triple("＋","Операция",0),Triple("▱","Объект",1),Triple("♙","Работники",2),Triple("▣","Материалы",3))
  for((symbol,label,index) in actionNames){
   val c=col().apply{background=shape(if(index==0)navy else Color.WHITE,17);setPadding(dp(5),dp(15),dp(5),dp(14));gravity=Gravity.CENTER;elevation=dp(1).toFloat()}
   add(c,text(symbol,25f,if(index==0)Color.WHITE else navy,true).apply{gravity=Gravity.CENTER})
   add(c,text(label,11f,if(index==0)Color.WHITE else navy,true).apply{gravity=Gravity.CENTER},7)
   c.setOnClickListener{when(index){0->edit();1->{page=1;render()};2->{page=3;render()};else->edit()}}
   actions.addView(c,LinearLayout.LayoutParams(0,dp(91),1f).apply{marginStart=dp(4);marginEnd=dp(4)})
  }
  add(body,actions,17)
  sectionLink("Объекты","Все объекты"){page=1;render()}
  val names=list.map{it.project}.distinct()
  if(names.isEmpty())empty("Нет объектов","Добавь первую операцию, чтобы начать учёт.")
  else names.take(3).forEach{projectCard(it,list)}
  sectionLink("Последние операции","Все операции"){page=2;render()}
  if(list.isEmpty())empty("Пока нет операций","Доходы и расходы появятся здесь.")
  else list.take(5).forEach{operationCard(it)}
 }
 private fun empty(a:String,b:String){val c=card();add(c,text(a,16f,navy,true));add(c,text(b,13f,muted),7);add(body,c,10)}
 private fun projectCard(name:String,list:List<Operation>){
  val ops=list.filter{it.project==name}
  val income=ops.filter{it.kind=="Доход"}.sumOf{it.amount}
  val spent=ops.filter{it.kind!="Доход"}.sumOf{it.amount}
  val c=card()
  add(c,text("▱   Объект "+name+"     ›",17f,navy,true))
  add(c,text("Финансовый результат",12f,muted),12)
  add(c,text(eur(income-spent),22f,if(income>=spent)green else red,true),5)
  val strip=row().apply{background=shape(if(income>=spent)Color.rgb(234,248,241) else Color.rgb(255,240,242),12);setPadding(dp(12),dp(12),dp(12),dp(12))}
  strip.addView(text("Доходы  "+eur(income),12f,green,true),LinearLayout.LayoutParams(0,-2,1f))
  strip.addView(text("Расходы  "+eur(spent),12f,red,true))
  add(c,strip,12)
  c.setOnClickListener{filter=name;page=2;render()}
  add(body,c,10)
 }
 private fun projects(){
  title("Строительные объекты")
  add(body,text("Финансы по каждому проекту",13f,muted),5)
  add(body,button("＋  Добавить объект"){edit()},18)
  val list=records()
  val names=list.map{it.project}.distinct()
  if(names.isEmpty())empty("Объектов пока нет","Создай первый объект через новую операцию.")
  else names.forEach{projectCard(it,list)}
 }
 private fun operations(){
  title("История операций")
  add(body,button("＋  Новая операция"){edit()},16)
  val list=records()
  val names=listOf("Все объекты")+list.map{it.project}.distinct()
  if(filter !in names)filter="Все объекты"
  val select=Spinner(this)
  select.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
  select.setSelection(names.indexOf(filter))
  select.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
   override fun onNothingSelected(p:android.widget.AdapterView<*>?){}
   override fun onItemSelected(p:android.widget.AdapterView<*>?,v:View?,pos:Int,id:Long){if(filter!=names[pos]){filter=names[pos];render()}}
  }
  val box=card()
  add(box,text("Фильтр по объекту",13f,muted))
  add(box,select,5)
  add(body,box,15)
  val selected=if(filter=="Все объекты")list else list.filter{it.project==filter}
  if(selected.isEmpty())empty("Записей нет","Нажми «Новая операция», чтобы добавить запись.")
  else selected.forEach{operationCard(it)}
 }
 private fun operationCard(e:Operation){
  val c=card()
  val r=row()
  val sign=if(e.kind=="Доход")"↙" else "↗"
  val color=if(e.kind=="Доход")green else red
  r.addView(text(sign,24f,color,true).apply{gravity=Gravity.CENTER;background=shape(if(e.kind=="Доход")Color.rgb(229,248,241) else Color.rgb(255,238,240),13)},LinearLayout.LayoutParams(dp(43),dp(43)))
  val info=col().apply{setPadding(dp(11),0,0,0)}
  add(info,text(e.category,15f,navy,true))
  add(info,text(e.project+" · "+SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(Date(e.created)),11f,muted),4)
  if(e.note.isNotBlank())add(info,text(e.note,12f,muted),3)
  r.addView(info,LinearLayout.LayoutParams(0,-2,1f))
  r.addView(text((if(e.kind=="Доход")"+" else "−")+eur(e.amount),14f,color,true))
  add(c,r)
  c.setOnClickListener{edit(e)}
  add(body,c,9)
 }
 private fun analytics(){
  val list=records()
  title("Финансовые отчёты")
  val income=list.filter{it.kind=="Доход"}.sumOf{it.amount}
  val spent=list.filter{it.kind!="Доход"}.sumOf{it.amount}
  val result=card()
  add(result,text("Чистый результат",13f,muted))
  add(result,text(eur(income-spent),28f,if(income>=spent)green else red,true),9)
  add(body,result,15)
  section("Расходы по категориям")
  val groups=list.filter{it.kind!="Доход"}.groupBy{it.category}
  if(groups.isEmpty())empty("Расходов пока нет","Здесь появится разбивка расходов.")
  else groups.toList().sortedByDescending{it.second.sumOf{e->e.amount}}.forEach{(name,ops)->
   val c=card()
   val r=row()
   r.addView(text(name,15f,navy,true),LinearLayout.LayoutParams(0,-2,1f))
   r.addView(text(eur(ops.sumOf{it.amount}),15f,red,true))
   add(c,r);add(body,c,9)
  }
  section("Выплаты работникам")
  val workers=list.filter{it.kind!="Доход"&&it.category=="Работники"}.groupBy{it.note.ifBlank{"Без имени"}}
  if(workers.isEmpty())empty("Нет выплат","Записывай имя работника при добавлении выплаты.")
  else workers.forEach{(name,ops)->
   val c=card();add(c,text(name,15f,navy,true));add(c,text("Выплачено: "+eur(ops.sumOf{it.amount}),14f,red),7);add(body,c,9)
  }
 }
 private fun input(hint:String,value:String="")=EditText(this).apply{this.hint=hint;setText(value);setSingleLine(true);textSize=15f}
 private fun edit(old:Operation?=null){
  val box=col().apply{setPadding(dp(24),dp(5),dp(24),0)}
  val types=arrayOf("Расход","Доход")
  val kind=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types);setSelection(types.indexOf(old?.kind?:"Расход"))}
  add(box,text("Тип операции",12f,muted));add(box,kind)
  val categories=arrayOf("Материалы","Работники","Транспорт","Инструменты","Прочее","Оплата клиента","Аванс клиента")
  val category=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,categories);setSelection(categories.indexOf(old?.category?:"Материалы").coerceAtLeast(0))}
  add(box,text("Категория",12f,muted),9);add(box,category)
  val project=input("Объект / адрес",old?.project?:"")
  val note=input("Описание / имя работника",old?.note?:"")
  val amount=input("Сумма €",old?.amount?.toString()?:"").apply{inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL}
  add(box,project,8);add(box,note,7);add(box,amount,7)
  val dialog=AlertDialog.Builder(this).setTitle(if(old==null)"Новая операция" else "Редактировать операцию").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null)
  if(old!=null)dialog.setNeutralButton("Удалить"){_,_->AlertDialog.Builder(this).setMessage("Удалить запись?").setNegativeButton("Отмена",null).setPositiveButton("Удалить"){_,_->db.writableDatabase.delete("entries","id=?",arrayOf(old.id.toString()));render()}.show()}
  val d=dialog.create()
  d.setOnShowListener{
   d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
    val n=amount.text.toString().replace(',','.').toDoubleOrNull()
    if(n==null||!n.isFinite()||n<=0||project.text.isBlank()){Toast.makeText(this,"Укажи объект и корректную сумму",Toast.LENGTH_LONG).show();return@setOnClickListener}
    val values=ContentValues().apply{put("kind",kind.selectedItem.toString());put("category",category.selectedItem.toString());put("project",project.text.toString().trim());put("note",note.text.toString().trim());put("amount",n);if(old==null)put("created",System.currentTimeMillis())}
    if(old==null)db.writableDatabase.insert("entries",null,values) else db.writableDatabase.update("entries",values,"id=?",arrayOf(old.id.toString()))
    d.dismiss();render()
   }
  }
  d.show()
 }
 private fun options(){
  AlertDialog.Builder(this).setTitle("Build Finance").setItems(arrayOf("Экспорт CSV","О приложении")){_,index->
   if(index==0)export() else AlertDialog.Builder(this).setMessage("Build Finance 2.0\nУчёт финансов строительных объектов.\nДанные сохраняются на устройстве.").setPositiveButton("OK",null).show()
  }.show()
 }
 private var exportData:ByteArray?=null
 private fun export(){
  fun quote(s:String)="\""+s.replace("\"","\"\"")+"\""
  val csv=buildString{
   append("\uFEFFДата;Тип;Категория;Объект;Описание;Сумма EUR\n")
   for(e in records()){
    val cells=listOf(SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date(e.created)),e.kind,e.category,e.project,e.note,e.amount.toString())
    append(cells.joinToString(";"){quote(it)}).append("\n")
   }
  }
  exportData=csv.toByteArray(Charsets.UTF_8)
  val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="text/csv";putExtra(Intent.EXTRA_TITLE,"build-finance.csv")}
  startActivityForResult(intent,901)
 }
 @Deprecated("Deprecated in Java")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==901){
   if(resultCode==RESULT_OK&&data?.data!=null){
    try{contentResolver.openOutputStream(data.data!!)?.use{it.write(exportData?:byteArrayOf())};Toast.makeText(this,"CSV сохранён",Toast.LENGTH_SHORT).show()}
    catch(e:Exception){Toast.makeText(this,"Ошибка экспорта: "+e.message,Toast.LENGTH_LONG).show()}
   }
   exportData=null
  }
 }
}
