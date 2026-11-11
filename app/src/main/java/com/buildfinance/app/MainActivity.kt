package com.buildfinance.app

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.view.*
import android.widget.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class Db(ctx: Context): SQLiteOpenHelper(ctx,"finance.db",null,1) {
 override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, category TEXT NOT NULL, project TEXT NOT NULL, note TEXT NOT NULL, amount REAL NOT NULL, created INTEGER NOT NULL)") }
 override fun onUpgrade(db: SQLiteDatabase,oldVersion:Int,newVersion:Int) {}
}
class MainActivity: Activity() {
 private lateinit var db:Db
 private lateinit var root:LinearLayout
 private val money=NumberFormat.getCurrencyInstance(Locale.GERMANY)
 private fun label(s:String,size:Float=17f):TextView = TextView(this).apply {text=s;textSize=size;setTextColor(Color.rgb(29,42,55));setPadding(10,12,10,12)}
 private fun field(hint:String)=EditText(this).apply {this.hint=hint;setSingleLine(true)}
 private fun add(view:View){root.addView(view)}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);db=Db(this);render()}
 private fun render(){
  root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(22,30,22,10);setBackgroundColor(Color.rgb(246,248,251))}
  val outer=ScrollView(this);outer.addView(root);setContentView(outer)
  add(label("🏗 Build Finance",27f))
  var income=0.0;var expense=0.0
  val cursor=db.readableDatabase.rawQuery("SELECT kind,amount FROM entries",null)
  cursor.use {while(it.moveToNext()){if(it.getString(0)=="Доход")income+=it.getDouble(1) else expense+=it.getDouble(1)}}
  add(label("Баланс: ${money.format(income-expense)}",24f));add(label("Доходы: ${money.format(income)}     Расходы: ${money.format(expense)}",16f))
  add(Button(this).apply{text="＋ Добавить операцию";setOnClickListener{entryDialog()}})
  add(label("Объекты и прибыль",21f))
  val p=db.readableDatabase.rawQuery("SELECT project,SUM(CASE WHEN kind='Доход' THEN amount ELSE -amount END) FROM entries GROUP BY project ORDER BY project",null)
  p.use {while(it.moveToNext())add(label("${it.getString(0)}: ${money.format(it.getDouble(1))}"))}
  add(label("История операций (нажмите для изменения)",19f))
  val c=db.readableDatabase.rawQuery("SELECT id,kind,category,project,note,amount,created FROM entries ORDER BY created DESC,id DESC",null)
  c.use {while(it.moveToNext()){
   val id=it.getLong(0);val kind=it.getString(1);val cat=it.getString(2);val project=it.getString(3);val note=it.getString(4);val amount=it.getDouble(5)
   val date=SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(Date(it.getLong(6)))
   add(Button(this).apply {text="$date · $project\n$cat · $note\n${if(kind=="Доход")"+" else "−"}${money.format(amount)}";isAllCaps=false;setOnClickListener{entryDialog(id,kind,cat,project,note,amount)}})
  }}
 }
 private fun entryDialog(id:Long?=null,oldKind:String="Расход",oldCat:String="Материалы",oldProject:String="",oldNote:String="",oldAmount:Double=0.0){
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,8,30,0)}
  val kind=Spinner(this);val kinds=arrayOf("Расход","Доход");kind.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,kinds);kind.setSelection(kinds.indexOf(oldKind));box.addView(kind)
  val cat=Spinner(this);val cats=arrayOf("Материалы","Работники","Транспорт","Инструменты","Прочее","Оплата клиента","Аванс клиента");cat.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,cats);cat.setSelection(cats.indexOf(oldCat).coerceAtLeast(0));box.addView(cat)
  val project=field("Объект / адрес");project.setText(oldProject);box.addView(project)
  val note=field("Описание / имя работника");note.setText(oldNote);box.addView(note)
  val amount=field("Сумма €");amount.inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; if(id!=null)amount.setText(oldAmount.toString());box.addView(amount)
  val dialog=AlertDialog.Builder(this).setTitle(if(id==null)"Новая операция" else "Изменить операцию").setView(box)
   .setPositiveButton("Сохранить",null).setNegativeButton("Отмена",null)
  if(id!=null)dialog.setNeutralButton("Удалить"){_,_->AlertDialog.Builder(this).setMessage("Удалить операцию?").setPositiveButton("Удалить"){_,_->db.writableDatabase.delete("entries","id=?",arrayOf(id.toString()));render()}.setNegativeButton("Отмена",null).show()}
  val d=dialog.create();d.setOnShowListener {d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
   val value=amount.text.toString().replace(',','.').toDoubleOrNull()
   if(value==null || value<=0 || project.text.isBlank()){Toast.makeText(this,"Укажите объект и сумму больше нуля",Toast.LENGTH_LONG).show();return@setOnClickListener}
   val values=ContentValues().apply{put("kind",kind.selectedItem.toString());put("category",cat.selectedItem.toString());put("project",project.text.toString().trim());put("note",note.text.toString().trim());put("amount",value);if(id==null)put("created",System.currentTimeMillis())}
   if(id==null)db.writableDatabase.insert("entries",null,values) else db.writableDatabase.update("entries",values,"id=?",arrayOf(id.toString()))
   d.dismiss();render()
  }};d.show()
 }
}
