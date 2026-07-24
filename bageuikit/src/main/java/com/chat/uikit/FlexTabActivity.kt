//package com.chat.uikit
//
//import android.view.Gravity
//import android.view.KeyEvent
//import androidx.fragment.app.Fragment
//import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
//import com.chat.base.adapter.BageFragmentStateAdapter
//import com.chat.base.base.BageBaseActivity
//import com.chat.base.endpoint.EndpointCategory
//import com.chat.base.endpoint.EndpointManager
//import com.chat.base.endpoint.entity.TabItemMenu
//import com.chat.base.utils.LayoutHelper
//import com.chat.uikit.databinding.ActFlexTabLayoutBinding
//
//class FlexTabActivity : BageBaseActivity<ActFlexTabLayoutBinding>() {
//    override fun getViewBinding(): ActFlexTabLayoutBinding {
//        return ActFlexTabLayoutBinding.inflate(layoutInflater)
//    }
//
//    @Override
//    override fun supportSlideBack(): Boolean {
//        return false
//    }
//
//    override fun initView() {
//        super.initView()
//        val tabItemMenus :List<TabItemMenu> = EndpointManager.getInstance().invokes(EndpointCategory.homeTab,this)
//        val fragments = ArrayList<Fragment>()
//        for (item in tabItemMenus){
//            bageVBinding.bottomView.addView(
//                item.itemTab,
//                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER)
//            )
//            fragments.add(item.fragment)
//        }
//        bageVBinding.vp.adapter = BageFragmentStateAdapter(this, fragments)
//        bageVBinding.vp.registerOnPageChangeCallback(object : OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                val list: List<String> = EndpointManager.getInstance()
//                    .invokes(EndpointCategory.homeUpdateSelect, tabItemMenus[position].itemTab.tag)
//            }
//        })
//        EndpointManager.getInstance()
//            .setMethod(
//                "home", EndpointCategory.homeUpdateSelect
//            ) { `object` ->
//                if (`object` != null && `object` is String) {
//                    for (i in tabItemMenus.indices) {
//                        if (tabItemMenus[i].itemTab.tag != null && tabItemMenus[i].itemTab.tag is String && tabItemMenus[i].itemTab.tag == `object`) {
//                            bageVBinding.vp.currentItem = i
//                            break
//                        }
//                    }
//
//
//                }
//                null
//            }
//    }
//
//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            moveTaskToBack(true);
//            return true
//        }
//        return super.onKeyDown(keyCode, event)
//    }
//
//    override fun finish() {
//        super.finish()
//        EndpointManager.getInstance().remove("tab_activity");
//    }
//}