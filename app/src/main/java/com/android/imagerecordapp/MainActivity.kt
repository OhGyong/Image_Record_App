package com.android.imagerecordapp

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.ContextMenu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.android.imagerecordapp.adapter.MainGridAdapter
import com.android.imagerecordapp.data.GridViewData
import com.android.imagerecordapp.data.GridViewDatabase
import com.android.imagerecordapp.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import java.sql.Date

@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: GridViewDatabase
    private lateinit var viewModel: MainViewModel
    private lateinit var mAdapter: MainGridAdapter

    private lateinit var imgUrl: String

    /**
     * 갤러리 불러오기
     */
    private val resultListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            // 사진 선택 취소할 경우
            if(it.resultCode == RESULT_CANCELED){
                Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show()
            }else{
                val uri = it.data!!.data!!

                // 권한 허용으로 에러 처리.
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                val cursor: Cursor = contentResolver.query(uri,null,null,null,null)!!
                cursor.moveToFirst()
                val path = cursor.getLong(3) // 날짜 정보

                // 데이터 베이스에 사진 정보 저장
                viewModel.inputImageData(db, Date(path).toString(), uri.toString())
                viewModel.getImageListData(db)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("onCreate")

        // 다크모드 비활성화
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 바인딩 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뷰모델 초기화
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // room의 db 초기화
        db = Room.databaseBuilder(
            applicationContext, GridViewDatabase::class.java, "database"
        ).build()

        // 이미지 뷰 observe
        viewModel.imageList.observe(this) {
            println("imageList observe")
            mAdapter = MainGridAdapter(it)
            binding.viewGrid.adapter = mAdapter

            // 아이템 롱 클릭으로 삭제
            mAdapter.setOnItemClickListener(object: MainGridAdapter.OnItemClickListener{
                override fun onItemClick(v: View, data: GridViewData, pos: Int) {
                    imgUrl = data.imgUri
                    v.setOnLongClickListener { false }
                    v.setOnCreateContextMenuListener(this@MainActivity)
                }
            })
        }

        // 이미지 추가
        binding.imageRecord.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "image/*"
            resultListener.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getImageListData(db)
    }

    // 컨텍스트 메뉴 띄우기
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val item = menu!!.add(0,0,0,"삭제")

        // 메뉴의 item 선택 시 해당 이미지 삭제
        item.setOnMenuItemClickListener {
            MainViewModel().deleteImageData(db, imgUrl)
            Toast.makeText(binding.root.context, "삭제 되었습니다.", Toast.LENGTH_SHORT).show()

            // 화면 새로고침
            viewModel.getImageListData(db)
            true
        }
    }
}