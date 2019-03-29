package com.mahmoud.kadapter

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlin.reflect.KClass

class KAdapter(defaults: Defaults = Defaults()) : RecyclerView.Adapter<KAdapter.MainViewHolder>() {
	private val TAG = this::class.simpleName
	private var items: ArrayList<Any>? = null
	private var viewsCount: Int = 0
	private var holderViews: List<View?>
	private var holder: MainViewHolder? = null
	private var viewTypes = ArrayList<Pair<KClass<*>, Int>>()
	private var userViewBindings: ArrayList<Pair<Int, ((view: View?, item: Any, position: Int) -> Unit)?>>
	private var currentBindingIndex = 0
	private var withClick = false
	private var onClickAction: ((Int) -> Unit)? = null
	private var withLongClick = false
	private var onLongClickAction: ((Int) -> Unit)? = null
	private var simpleAdapterInitialized = false
	
	//-------------------
	private  var defaults:Defaults
	//----------------------------------------
	private var rv: RecyclerView? = null
	private var orientation = DividerItemDecoration.VERTICAL
	private var isHorizontal = false
	private var lastDivider: RecyclerView.ItemDecoration? = null
	private var onLoadSet = false
	private var isLoading = false
	private var onLoadAction: ((lastPosition: Int) -> Unit)? = null
	private var dummyViewPosition: Int? = null
	//marking
	private var drawable: GradientDrawable? = null
	private var markedPositions: ArrayList<Int>? = null

	
	init {
		userViewBindings = ArrayList()
		//just empty initialization in case no bind() not called
		for (i in 0..19)
			userViewBindings.add(0 to null)   //dumy id with null action ;
		holderViews = mutableListOf<View?>()
		markedPositions = ArrayList()
		this.defaults=defaults
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
		if (viewTypes.isNullOrEmpty()) throw Exception("No resource layouts specified ")
		if (viewType > viewTypes.size - 1) throw Exception("No resource layouts specified for some data types ")
		val inflator = LayoutInflater.from(parent.context)
		val row = inflator.inflate(viewTypes[viewType].second, parent, false)
		val mainViewHolder = MainViewHolder(row)
		if (withClick) {
			mainViewHolder.itemView.setOnClickListener {
				onClickAction?.invoke(mainViewHolder.adapterPosition)
			}
		}
		
		if (withLongClick) {
			mainViewHolder.itemView.setOnLongClickListener {
				onLongClickAction?.invoke(mainViewHolder.adapterPosition)
				return@setOnLongClickListener true
			}
		}
		return mainViewHolder
	}
	
	override fun getItemCount(): Int {
		return if (items == null) 0 else items!!.size
	}
	
	override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
		//remove marking if any
		if (!markedPositions!!.isNullOrEmpty()) {
			//search for current position in marked positions and remove overlay if not found
			if (markedPositions!!.contains(position)) {
				holder.itemView.overlay.add(drawable!!)
			} else {
				holder.itemView.overlay.clear()
			}
		}
		
		if (items.isNullOrEmpty()) {
			Log.e("KAdapter", "Data is null or empty")
			return
		}
		if (userViewBindings.isNullOrEmpty()) return
		this.holder = holder
		holderViews = listOf(
			holder.view1,
			holder.view2,
			holder.view3,
			holder.view4,
			holder.view5,
			holder.view6,
			holder.view7,
			holder.view8,
			holder.view9,
			holder.view10,
			holder.view11,
			holder.view12,
			holder.view13,
			holder.view14,
			holder.view15,
			holder.view16,
			holder.view17,
			holder.view18,
			holder.view19,
			holder.view20
		)
		
		for (i in 0 until viewsCount) {
			holderViews.get(i) ?: continue
			
			userViewBindings[i].second!!(holderViews.get(i), items!!.get(position), position)
		}
		if (onLoadSet and (items!!.size - position - 2 == this.defaults.loadMoreThreshold) and !isLoading) {
			dummyViewPosition = position + this.defaults.loadMoreThreshold + 1
			
			onLoadAction?.invoke(position + this.defaults.loadMoreThreshold)
			isLoading = true
		}
	}
	
	override fun getItemViewType(position: Int): Int {
		var i = -1
		viewTypes.forEach {
			if ((items!!.get(position))::class == it.first) {
				return i + 1
			}
			i++
		}
		return i + 1
	}
	
	fun setData(data: ArrayList<*>): KAdapter {
		this.items = data as ArrayList<Any>
		if (onLoadSet) {
			this.addViewType(Dummy::class to this.defaults.loadMoreDummyViewResId)
			this.items!!.add(Dummy())
			notifyItemInserted(this.items!!.size - 1)
		}
		notifyDataSetChanged()
		return this
	}
	
	fun bind(binding: Pair<Int, (view: View?, item: Any, position: Int) -> Unit>): KAdapter {
		if (simpleAdapterInitialized) {
			//	Log.e(TAG, " bind(Int) : Simple Adapter already initialized ..ignoring")
			return this
		}
		
		if (currentBindingIndex > 19) {
			Log.e(TAG, " bind(Int) : Currently Maximum 20 view bindings allowed")
			return this
		}
		userViewBindings[currentBindingIndex] = (binding.first to binding.second)
		
		currentBindingIndex++
		viewsCount++
		return this
	}
	
	fun addViewType(type: Pair<KClass<*>, Int>): KAdapter {
		this.viewTypes.add(type)
		return this
	}
	
	fun onClick(onClickAction: (Int) -> Unit): KAdapter {
		this.onClickAction = onClickAction
		withClick = true
		return this
	}
	
	fun onLongClick(onLongClickAction: (Int) -> Unit): KAdapter {
		this.onLongClickAction = onLongClickAction
		withLongClick = true
		return this
	}
	
	fun onLoadMore(onLoadMoreAction: (lastPosition: Int) -> Unit): KAdapter {
		onLoadSet = true
		this.onLoadAction = onLoadMoreAction
		return this
	}
	
	fun resetLoadMore(newAddedSize: Int) {
		this.notifyItemRangeInserted(this.itemCount, this.itemCount + newAddedSize - 1)
		this.items?.removeAt(dummyViewPosition!!)
		this.notifyItemRemoved(dummyViewPosition!!)
		this.notifyItemRangeChanged(dummyViewPosition!!, getItemCount())
		
		
		if (newAddedSize > 0) {
			this.items?.add(Dummy())
			notifyItemInserted(this.items!!.size - 1)
			this.isLoading = false
		} else {
			this.onLoadSet = false
		}
	}
	
	//return view in same itemview if needed , like editBox or seekBar
	fun getViewByIdAt(viewID: Int, position: Int): View? {
		return this.rv?.layoutManager?.findViewByPosition(position)?.findViewById(viewID)
	}
	
	fun markItemAt(position: Int) {
		if (markedPositions?.contains(position)!!) {
			//Log.e(TAG, "already marked  $position ignoring")
			return
		}
		val view = this.rv?.findViewHolderForAdapterPosition(position)?.itemView
		view?.let {
		
			val overlay = it.overlay
			drawable = it.context.getDrawable(R.drawable.overlay) as GradientDrawable
			
			drawable?.setBounds(0, 0, it.measuredWidth, it.measuredHeight)
			drawable?.setStroke(this.defaults.markedItemStrokeWidth,this.defaults.MarkedItemStrokeColor)
			overlay.add(drawable!!)
			var animator = ObjectAnimator.ofPropertyValuesHolder(drawable, PropertyValuesHolder.ofInt("alpha", 0, 255));
			animator.setTarget(drawable);
			animator.setDuration(300);
			animator.start()
			markedPositions?.add(position)
		}
	}
	
	fun unMarkItemAt(position: Int) {
		if (markedPositions.isNullOrEmpty() || !(markedPositions?.contains(position)!!)) return
	
		val view = this.rv?.findViewHolderForAdapterPosition(position)?.itemView
		view?.let { it ->
			it.overlay.also { it.clear() }
			markedPositions?.remove(position)
		}
	}
	
	fun getMarkedPositions(): ArrayList<Int> {
		return markedPositions!!
	}
	
	fun simpleAdapter(): KAdapter {
		this.addViewType(String::class to R.layout.kadapter_simple_item)
		this.bind(R.id.kadapter_simple_item_text to { view, item, position ->
			(view as TextView).text = item as String
		})
		simpleAdapterInitialized = true
		return this
	}
	
	//-------------------------------------------------------------
	// functions related to RecyclerView
	//-------------------------------------------------------------
	fun attachTo(recyclerView: RecyclerView): KAdapter {
		this.rv = recyclerView.also {
			it.adapter = this
			it.layoutManager = LinearLayoutManager(it.context)
		}
		return this
	}
	
	fun onSwipe(onSwipeLambda: (position: Int, direction: Int) -> Unit): KAdapter {
		val touchHandler =
			ItemTouchHelper(SwipeHandler(onSwipeLambda, 0, this.defaults.swipeDirs))
		touchHandler.attachToRecyclerView(this.rv)
		return this
	}
	
	fun asVertical(): KAdapter {
		this.rv?.let {
			it.layoutManager = LinearLayoutManager(it.context)
			isHorizontal = false
		}
		return this
	}
	
	fun asHorizontal(): KAdapter {
		this.rv?.let {
			it.layoutManager = LinearLayoutManager(it.context, LinearLayoutManager.HORIZONTAL, false)
			isHorizontal = true
		}
		return this
	}
	
	fun asGrid(numOfColumns: Int? = 0): KAdapter {
		this.rv?.let {
			val columns = if (numOfColumns == 0) calculateNoOfColumns() else numOfColumns
			it.layoutManager = GridLayoutManager(it.context, columns!!)
			isHorizontal = false
		}
		return this
	}
	
	fun asHorizontalGrid(numOfRows: Int): KAdapter {
		this.rv?.let {
			it.layoutManager = GridLayoutManager(it.context, numOfRows, GridLayoutManager.HORIZONTAL, false)
			isHorizontal = true
		}
		return this
	}
	
	fun asStaggered(numOfColumns: Int? = 0): KAdapter {
		this.rv?.let {
			val columns = if (numOfColumns == 0) calculateNoOfColumns() else numOfColumns
			it.layoutManager = StaggeredGridLayoutManager(columns!!, StaggeredGridLayoutManager.VERTICAL)
			isHorizontal = false
		}
		return this
	}
	
	fun asHorizontalStaggered(numOfRows: Int): KAdapter {
		this.rv?.let {
			it.layoutManager = StaggeredGridLayoutManager(numOfRows, StaggeredGridLayoutManager.HORIZONTAL)
			isHorizontal = true
		}
		return this
	}
	
	fun withDivider(divider: DividerItemDecoration? = null): KAdapter {
		this.rv?.let {
			lastDivider?.let { it1 -> it.removeItemDecoration(it1) }
			if (divider == null) {
				orientation = if (isHorizontal) DividerItemDecoration.HORIZONTAL else DividerItemDecoration.VERTICAL
				val decor = DividerItemDecoration(it.context, orientation)
				
				it.addItemDecoration(decor)
				lastDivider = decor
			} else {
				it.addItemDecoration(divider)
				lastDivider = divider
			}
		}
		
		
		return this
	}
	
	fun withItemDecoration(itemDecoration: RecyclerView.ItemDecoration? = null): KAdapter {
		this.rv?.let {
		
		var decor: RecyclerView.ItemDecoration
		if (itemDecoration == null) {
			decor = SimpleItemDecoration(it.context)
		} else {
			decor = itemDecoration
		}
		it.addItemDecoration(decor)
		}
		return this
	}
	
	private fun calculateNoOfColumns(): Int? {
		val displayMetrics = Resources.getSystem().getDisplayMetrics()
		val dpWidth = displayMetrics.widthPixels.div(displayMetrics.density)
		return (dpWidth.div(160 + 0.5)).toInt()
	}
	
	inner class MainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		var view1: View? = view.findViewById(userViewBindings.get(0).first)
		var view2: View? = view.findViewById(userViewBindings.get(1).first)
		var view3: View? = view.findViewById(userViewBindings.get(2).first)
		var view4: View? = view.findViewById(userViewBindings.get(3).first)
		var view5: View? = view.findViewById(userViewBindings.get(4).first)
		var view6: View? = view.findViewById(userViewBindings.get(5).first)
		var view7: View? = view.findViewById(userViewBindings.get(6).first)
		var view8: View? = view.findViewById(userViewBindings.get(7).first)
		var view9: View? = view.findViewById(userViewBindings.get(8).first)
		var view10: View? = view.findViewById(userViewBindings.get(9).first)
		var view11: View? = view.findViewById(userViewBindings.get(10).first)
		var view12: View? = view.findViewById(userViewBindings.get(11).first)
		var view13: View? = view.findViewById(userViewBindings.get(12).first)
		var view14: View? = view.findViewById(userViewBindings.get(13).first)
		var view15: View? = view.findViewById(userViewBindings.get(14).first)
		var view16: View? = view.findViewById(userViewBindings.get(15).first)
		var view17: View? = view.findViewById(userViewBindings.get(16).first)
		var view18: View? = view.findViewById(userViewBindings.get(17).first)
		var view19: View? = view.findViewById(userViewBindings.get(18).first)
		var view20: View? = view.findViewById(userViewBindings.get(19).first)
	}
	
	private inner class Dummy()
	inner class SwipeHandler(val onSwipeLambda: (position: Int, direction: Int) -> Unit,dragDirs: Int,swipeDirs: Int) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
		private var mBackground: ColorDrawable? = null
		private var backgroundColorLeft: Int = 0
		private var backgroundColorRight: Int = 0
		
		init {
			mBackground = ColorDrawable()
			backgroundColorLeft = defaults.swipeBackgroundColorLeft
			backgroundColorRight = defaults.swipeBackgroundColorRight
		}
		
		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			return false
		}
		
		override fun onChildDraw(
			c: Canvas,
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			dX: Float,
			dY: Float,
			actionState: Int,
			isCurrentlyActive: Boolean
		) {
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
			val itemView = viewHolder.itemView
			if (dX > 0) {
				// RIGHT swipe
				mBackground?.setColor(backgroundColorRight)
				mBackground?.setBounds(
					itemView.left, itemView.top,
					itemView.left + dX.toInt(),
					itemView.bottom
				)
			} else {
				// LEFT swipe
				mBackground?.setColor(backgroundColorLeft)
				mBackground?.setBounds(
					itemView.getRight() + dX.toInt(),
					itemView.getTop(), itemView.getRight(), itemView.getBottom()
				)
			}
			
			mBackground?.draw(c)
		}
		
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			onSwipeLambda.invoke(viewHolder.adapterPosition, direction)
		}
	}
	
	inner class SimpleItemDecoration(c: Context) : RecyclerView.ItemDecoration() {
		private val paintGray: Paint
		private val offset: Int
	
		
		init {
			offset = 10
			paintGray = Paint(Paint.ANTI_ALIAS_FLAG)
			paintGray.setColor(defaults.decorationColor)
			paintGray.setStyle(Paint.Style.STROKE)
			paintGray.setStrokeWidth(defaults.decorationStrokeWidth)
		}
		
		override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
			super.getItemOffsets(outRect, view, parent, state)
			outRect.set(offset, offset, offset, offset)
		}
		
		override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
			super.onDraw(c, parent, state)
		}
		
		override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
			super.onDrawOver(c, parent, state)
			val layoutManager = parent.layoutManager
			
			for (i in 0 until parent.childCount) {
				val child = parent.getChildAt(i)
				
				c.drawRoundRect(
					(layoutManager!!.getDecoratedLeft(child) + offset).toFloat(),
					(layoutManager.getDecoratedTop(child) + offset).toFloat(),
					(layoutManager.getDecoratedRight(child) - offset).toFloat(),
					(layoutManager.getDecoratedBottom(child) - offset).toFloat(),
					defaults.decorationCornersRadius,
					defaults.decorationCornersRadius,
					paintGray
				)
			}
		}
	}
	
	data class Defaults(
		var decorationColor: Int = Color.GRAY,
		var decorationStrokeWidth:Float=5f,
		var decorationCornersRadius:Float=10f,
		var loadMoreDummyViewResId: Int = R.layout.dummy_loading,
		var loadMoreThreshold: Int = 0,
		var swipeDirs: Int=(ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT),
		var swipeBackgroundColorLeft: Int = Color.parseColor("#FF0000"),
		var swipeBackgroundColorRight: Int = Color.parseColor("#26B600"),
		var markedItemStrokeWidth:Int=5,
		var MarkedItemStrokeColor:Int=Color.BLACK
	)
}