package com.batuhan.yemekkitabi.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.batuhan.yemekkitabi.databinding.FragmentTarifBinding
import com.batuhan.yemekkitabi.model.Tarif
import com.batuhan.yemekkitabi.roomdb.TarifDao
import com.batuhan.yemekkitabi.roomdb.TarifDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream


class TarifFragment : Fragment() {
    private var _binding: FragmentTarifBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    private lateinit var permissionLauncer: ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private var secilenGorsel :Uri?=null
    private var secilenBitmap : Bitmap?=null

    private lateinit var db:TarifDatabase
    private lateinit var tarifDao :TarifDao

    private val mDisposable = CompositeDisposable()
    private var secilenTarif : Tarif? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db = Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler").build()
        tarifDao = db.tarifDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTarifBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageView.setOnClickListener { gorselSec(it) }
        binding.buttonKaydet.setOnClickListener { kaydet(it) }
        binding.buttonSil.setOnClickListener { sil(it) }

        arguments?.let {
            val bilgi = TarifFragmentArgs.fromBundle(it).bilgi
            if (bilgi == "yeni") {
                //Yeni tarif kaydediliyor
                binding.buttonSil.isEnabled = false
                binding.buttonKaydet.isEnabled = true
                binding.isimText.setText("")
                binding.malzemeText.setText("")
            } else {
                //Eski eklenmiş tarif gösteriliyor
                binding.buttonSil.isEnabled = true
                binding.buttonKaydet.isEnabled = false
                val id = TarifFragmentArgs.fromBundle(it).id

                mDisposable.add(
                    tarifDao.findById(id).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponse)
                )
            }
        }
        
    }
    private fun handleResponse(tarif:Tarif){
        val bitmap = BitmapFactory.decodeByteArray(tarif.gorsel,0,tarif.gorsel.size)
        binding.imageView.setImageBitmap(bitmap)

        binding.isimText.setText(tarif.isim)
        binding.malzemeText.setText(tarif.malzeme)
        secilenTarif = tarif
    }
    fun kaydet(view: View) {
        val isim = binding.isimText.text.toString()
        val malzeme = binding.malzemeText.text.toString()

        if (secilenBitmap!=null){
            val kucukBipMap = kucukBipMapOlustur(secilenBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            kucukBipMap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi = outputStream.toByteArray()

            val tarif = Tarif(isim,malzeme,byteDizisi)
            //RXJAVA
            mDisposable.add(
                tarifDao.insert(tarif)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )


        }
    }
    private fun  handleResponseForInsert(){
        //bir önceki fragmante dön
        val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)

    }

    fun sil(view: View) {
        if (secilenTarif!= null){
            mDisposable.add(
                tarifDao.delete(tarif = secilenTarif!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )
        }

    }

    fun gorselSec(view: View) {

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
                //İZİN VERİLMEMİŞ , İZİN İSTEMEMİZ GEREKİYOR
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_MEDIA_IMAGES)){
                    //snackbar göstermemiz gerekiyor, kullanıcıdan neden izin istediğimizi bir kez daha söyleyerek izin almamız gerek
                    Snackbar.make(view,"Galeriye ulaşıp görsel seçmemiz lazım!",Snackbar.LENGTH_INDEFINITE).setAction(
                        "İzin Ver",
                        View.OnClickListener {
                            //izin isteyeceğiz
                            permissionLauncer.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }

                    ).show()
                }else{
                    //izin isteyeceğiz
                    permissionLauncer.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else{
                //izin verilmiş , galeriden gidebiliriz
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        }else{
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                //İZİN VERİLMEMİŞ , İZİN İSTEMEMİZ GEREKİYOR
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //snackbar göstermemiz gerekiyor, kullanıcıdan neden izin istediğimizi bir kez daha söyleyerek izin almamız gerek
                    Snackbar.make(view,"Galeriye ulaşıp görsel seçmemiz lazım!",Snackbar.LENGTH_INDEFINITE).setAction(
                        "İzin Ver",
                        View.OnClickListener {
                            //izin isteyeceğiz
                            permissionLauncer.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }

                    ).show()
                }else{
                    //izin isteyeceğiz
                    permissionLauncer.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                //izin verilmiş , galeriden gidebiliriz
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        }
    }
    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode==AppCompatActivity.RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult!=null){
                    secilenGorsel = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >=28){
                            val source = ImageDecoder.createSource(requireActivity().contentResolver,secilenGorsel!!)
                                secilenBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(secilenBitmap)
                            }else{
                                secilenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,secilenGorsel)
                                binding.imageView.setImageBitmap(secilenBitmap)
                        }
                    }catch (e: Exception){
                        println(e.localizedMessage)
                    }
                }
            }

        }
        permissionLauncer = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if (result){
                //izin verildi galeriye gidebiliriz
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                //iZİN VERİLMEDİ
                Toast.makeText(requireContext(),"İzin verilmedi!",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun kucukBipMapOlustur(kullaniininSectigiBitmap: Bitmap , maximumBoyut:Int) : Bitmap{
        var width = kullaniininSectigiBitmap.width
        var height = kullaniininSectigiBitmap.height

        val bitMapOrani : Double = width.toDouble()/height.toDouble()

        if (bitMapOrani>1){
            //görsel yatay
            width = maximumBoyut
            val kisaltilmisYukseklik = width/bitMapOrani
            height = kisaltilmisYukseklik.toInt()
        }else{
            //görsel dikey
            height = maximumBoyut
            val kisaltilmisGenislik = height * bitMapOrani

            width = kisaltilmisGenislik.toInt()

        }
        return  Bitmap.createScaledBitmap(kullaniininSectigiBitmap,width,height,true)
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}