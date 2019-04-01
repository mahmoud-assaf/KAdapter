package com.mahmoud.kadapter

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.TextPaint
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
		
		
		
		if (newAddedSize > 0) {
			this.rv?.handler?.postDelayed({
				this.notifyItemRangeInserted(this.itemCount, this.itemCount + newAddedSize - 1)
				this.items?.removeAt(dummyViewPosition!!)
				this.notifyItemRemoved(dummyViewPosition!!)
				
				this.items?.add(Dummy())
				notifyItemInserted(this.items!!.size - 1)
				this.isLoading = false
				
			},500)
			
		} else {
			this.rv?.handler?.postDelayed({
			this.items?.removeAt(dummyViewPosition!!)
			this.notifyItemRemoved(dummyViewPosition!!)
			},500)
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
			drawable?.setStroke(convertDpToPixel(this.defaults.markedItemStrokeWidth).toInt(),this.defaults.markedItemStrokeColor)
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
		this.rv?.let {
			val touchHandler =
				ItemTouchHelper(SwipeHandler(it.context,onSwipeLambda, 0, this.defaults.swipeDirs))
			touchHandler.attachToRecyclerView(it)
		}
	
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
	inner class SwipeHandler(val mContext:Context,val onSwipeLambda: (position: Int, direction: Int) -> Unit,dragDirs: Int,swipeDirs: Int) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
		private var mBackground: ColorDrawable? = null
		private var backgroundColorLeft: Int = 0
		private var backgroundColorRight: Int = 0
		var leftpaint =  TextPaint()
		var leftswipetext=""
		var rightpaint=Paint()
		var rightswipetext=""
		private val textBoundsLeft=Rect()
		private val textBoundsRight=Rect()
		
		private var rightswipeDrawable:Drawable?=null
		private var rightintrinsicWidth=0
		private var rightintrinsicHeight=0
		var rightIconTop = 0
		var	rightIconMargin = 0
		var	rightIconLeft = 0
		var	rightIconRight = 0
		var	rightIconBottom = 0
		
		
		
		
		private var leftswipeDrawable:Drawable?=null
		private var leftintrinsicWidth=0
		private var leftintrinsicHeight=0
		var leftIconTop:Int=0
		var  leftIconMargin = 0
		var	leftIconLeft = 0
		var	leftIconRight =0
		var	leftIconBottom = 0
		
		init {
			mBackground = ColorDrawable()
			backgroundColorLeft = defaults.swipeBackgroundColorLeft
			backgroundColorRight = defaults.swipeBackgroundColorRight
			
			defaults.swipeTextLeft?.let {
				leftswipetext=it
				leftpaint.textAlign=Paint.Align.RIGHT
				leftpaint.setColor(defaults.swipeTextLeftColor);
				leftpaint.setStyle(Paint.Style.FILL);
				leftpaint.setTextSize(convertSPToPixel(defaults.swipeTextLeftSize));
				leftpaint.setTypeface(defaults.swipeTextLeftTypeface)
				leftpaint.getTextBounds(it,0,it.length,textBoundsLeft)
			}
			
			
			defaults.swipeTextRight?.let {
				rightswipetext=it
				rightpaint.textAlign=Paint.Align.LEFT
				rightpaint.setColor(defaults.swipeTextRightColor);
				rightpaint.setStyle(Paint.Style.FILL);
				rightpaint.setTextSize(convertSPToPixel(defaults.swipeTextRightSize));
				rightpaint.setTypeface(defaults.swipeTextRightTypeface)
				rightpaint.getTextBounds(it,0,it.length,textBoundsRight)
			}
			
			defaults.swipeLeftDrawable?.let {
				leftswipeDrawable= ContextCompat.getDrawable(mContext, it)
				if (leftswipeDrawable==null) Log.e("leftdraw","null") else Log.e("left draw","not null")
				
			}
			
			defaults.swipeRightDrawable?.let {
				rightswipeDrawable= ContextCompat.getDrawable(mContext, it)
				if (rightswipeDrawable==null) Log.e("rightdraw","null") else Log.e("right draw","not null")
				
			}
			
		}
		
		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			return false
		}
		
		override fun onChildDraw(c: Canvas,recyclerView: RecyclerView,viewHolder: RecyclerView.ViewHolder,dX: Float,dY: Float,actionState: Int,isCurrentlyActive: Boolean) {
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
			val itemView = viewHolder.itemView
			
			leftswipeDrawable?.let {
				leftintrinsicWidth = it.intrinsicWidth
				leftintrinsicHeight = it.intrinsicHeight
				 leftIconTop = itemView.getTop() + (itemView.height - leftintrinsicHeight) / 2;
				 leftIconMargin = (itemView.height - leftintrinsicHeight) / 2;
				 leftIconLeft = itemView.getRight() - leftIconMargin - leftintrinsicWidth
				 leftIconRight = itemView.getRight() - leftIconMargin
				 leftIconBottom = leftIconTop + leftintrinsicHeight
			}
			
			rightswipeDrawable?.let {
				rightintrinsicWidth = it.intrinsicWidth
				rightintrinsicHeight = it.intrinsicHeight
				rightIconTop = itemView.getTop() + (itemView.height - rightintrinsicHeight) / 2;
				rightIconMargin = (itemView.height - rightintrinsicHeight) / 2;
				rightIconLeft = itemView.getLeft() + rightIconMargin
				rightIconRight = itemView.getLeft() + rightIconMargin+ rightintrinsicWidth
				rightIconBottom = rightIconTop + rightintrinsicHeight
			}
		
			
			
			if (dX > 0) {
				Log.e(TAG,"right swip")
			
				// RIGHT swipe
				mBackground?.let {
					it.setColor(backgroundColorRight)
					it.setBounds(
						itemView.left, itemView.top,
						itemView.left + dX.toInt(),
						itemView.bottom
					)
					it.draw(c)
				}
				
				rightswipeDrawable?.let {
					it.setBounds(rightIconLeft, rightIconTop, rightIconRight, rightIconBottom)
					it.draw(c)
				}
				rightswipetext?.let {
					c.drawText(it, itemView?.left!!.toFloat()+convertDpToPixel(defaults.swipeTextRightMargin), itemView.top.toFloat()+(itemView.height/2)-textBoundsRight.exactCenterY(),rightpaint )
					
				}
				
			} else {
				Log.e(TAG,"left swip")
				/// LEFT swipe
				mBackground?.let {
					it.setColor(backgroundColorLeft)
					it.setBounds(
						itemView.getRight() + dX.toInt(),
						itemView.getTop(), itemView.getRight(), itemView.getBottom()
					)
					it.draw(c)
				}
				leftswipeDrawable?.let {
					it.setBounds(leftIconLeft, leftIconTop, leftIconRight, leftIconBottom)
					it.draw(c)
				}
				leftswipetext?.let {
					c.drawText(it, itemView?.right!!.toFloat()-convertDpToPixel(defaults.swipeTextLeftMargin), itemView.top.toFloat()+(itemView.height/2)-textBoundsLeft.exactCenterY(), leftpaint)
					
				}
			
			}
			
		}
		
	
		
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			onSwipeLambda.invoke(viewHolder.adapterPosition, direction)
		}
	}
	private fun convertDpToPixel(dp: Int): Float {
		val metrics = Resources.getSystem().getDisplayMetrics()
		val px = dp * (metrics.densityDpi / 160f)
		return Math.round(px).toFloat()
	}
	
	
	private fun convertSPToPixel(sp: Int): Float {
		val metrics = Resources.getSystem().getDisplayMetrics()
		val px = sp * metrics.scaledDensity
		return px
	}
	
	inner class SimpleItemDecoration(c: Context) : RecyclerView.ItemDecoration() {
		private val paintGray: Paint
		private val offset: Int
	
		
		init {
			offset = 10
			paintGray = Paint(Paint.ANTI_ALIAS_FLAG)
			paintGray.setColor(defaults.decorationColor)
			paintGray.setStyle(Paint.Style.STROKE)
			paintGray.setStrokeWidth(convertDpToPixel(defaults.decorationStrokeWidth))
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
		//------------decoration --------------------
		var decorationColor: Int = Color.GRAY,
		var decorationStrokeWidth:Int=2,
		var decorationCornersRadius:Float=10f,
		//-----------------loadMore--------------------------------
		var loadMoreDummyViewResId: Int = R.layout.dummy_loading,
		var loadMoreThreshold: Int = 0,
		//-----swiping------------------------
		var swipeDirs: Int=(ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT),
		var swipeBackgroundColorLeft: Int = Color.parseColor("#FFFFFF"),
		var swipeTextLeft:String?=null,
		var swipeTextLeftColor:Int=Color.parseColor("#000000"),
		var swipeTextLeftSize:Int=18,
		var swipeTextLeftTypeface: Typeface=Typeface.DEFAULT,
		var swipeTextLeftMargin:Int=10,
		
		var swipeBackgroundColorRight: Int = Color.parseColor("#FFFFFF"),
		var swipeTextRight:String?=null,
		var swipeTextRightColor:Int=Color.parseColor("#000000"),
		var swipeTextRightSize:Int=18,
		var swipeTextRightTypeface: Typeface=Typeface.DEFAULT,
		var swipeTextRightMargin:Int=10,
		
		var swipeLeftDrawable: Int?=null,
		var swipeRightDrawable: Int?=null,
		
		//---------marking------------------------------------------
		var markedItemStrokeWidth:Int=3,
		var markedItemStrokeColor:Int=Color.BLACK
	//--------------------------------------------------------------
	)
}
