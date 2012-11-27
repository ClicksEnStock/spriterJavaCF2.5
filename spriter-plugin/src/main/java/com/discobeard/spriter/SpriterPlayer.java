package com.discobeard.spriter;

import com.discobeard.spriter.dom.Animation;
import com.discobeard.spriter.dom.SpriterData;
import com.discobeard.spriter.draw.AbstractDrawer;
import com.discobeard.spriter.draw.DrawInstruction;
import com.discobeard.spriter.file.Reference;
import com.discobeard.spriter.objects.SpriterBone;
import com.discobeard.spriter.objects.SpriterKeyFrame;
import com.discobeard.spriter.objects.SpriterModObject;
import com.discobeard.spriter.objects.SpriterObject;

import java.util.List;

public class SpriterPlayer{

	private SpriterData spriterData;
	private Animation animation;
	private long frame = 0;
	private int frameSpeed = 30;
	private int animationIndex = 0;
	private int currentKey = 0;
	private DrawInstruction[] instructions;
	private List<SpriterKeyFrame[]> keyframes;
	private SpriterBone[] tempBones;
	private AbstractDrawer<?> drawer;
	private int currenObjectsToDraw;
	private int flipX = 1, flipY = 1;
	private float angle = 0;
	private float scale = 1f;
	private SpriterBone rootParent;
	private SpriterModObject[] moddedObjects,moddedBones;
	private SpriterKeyFrame lastFrame;
	private boolean transitionFixed = true;
	private int fixCounter = 0;
	private float xOffset, yOffset;
	
	
	
	public SpriterPlayer(SpriterData spriterData, AbstractDrawer<?> drawer,List<SpriterKeyFrame[]> keyframes){
		this.spriterData = spriterData;
		this.keyframes = keyframes;
		this.drawer = drawer;
		this.generatePool();
		this.animation = this.spriterData.getEntity().get(0).getAnimation().get(0);
		this.rootParent = new SpriterBone();
		this.rootParent.setScaleX(this.scale);
		this.rootParent.setScaleY(this.scale);
		this.lastFrame = new SpriterKeyFrame();
		
		SpriterBone[] tmpBones = this.tempBones.clone();
		SpriterObject[] tmpObjs = new SpriterObject[this.instructions.length];
		for(int i = 0; i < tmpObjs.length; i++){
			tmpObjs[i] = new SpriterObject();
		}
		this.lastFrame.setBones(tmpBones);
		this.lastFrame.setObjects(tmpObjs);
		
	}
	
	private void generatePool(){
		this.instructions = new DrawInstruction[SpriterKeyFrameProvider.MAX_OBJECTS];
		this.moddedObjects = new SpriterModObject[this.instructions.length];
		for(int i = 0; i < this.instructions.length; i++){
			this.instructions[i] = new DrawInstruction(new Reference(0,0),0,0,0,0,0,0,0,0);
			this.moddedObjects[i] = new SpriterModObject();
		}
		this.tempBones = new SpriterBone[SpriterKeyFrameProvider.MAX_BONES];
		this.moddedBones = new SpriterModObject[this.tempBones.length];
		for(int i = 0; i < this.tempBones.length; i++){
			this.tempBones[i] = new SpriterBone();
			this.moddedBones[i] = new SpriterModObject();
		}
	}
	
	/**
	 * Draws the current animation
	 */
	public void draw(){
		for(int i = 0; i< this.currenObjectsToDraw; i++){
			DrawInstruction dI = this.instructions[i];
			this.drawer.draw(dI);
		}
	}
	
	/**
	 * Updates this player at current animation index.
	 * @param xOffset
	 * @param yOffset
	 */
	public void update(float xOffset, float yOffset){
		//Fetch information
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		SpriterKeyFrame[] keyframes = this.keyframes.get(animationIndex);
		SpriterKeyFrame firstKeyFrame; 
		SpriterKeyFrame secondKeyFrame;
		if(this.transitionFixed){
			firstKeyFrame = keyframes[this.currentKey];
			secondKeyFrame = keyframes[(this.currentKey+1)%keyframes.length];
			
			//Update
			if(this.frame > this.animation.getLength())
				this.frame = 0;
			this.frame += this.frameSpeed;
			if (this.frame > keyframes[this.currentKey].getEndTime()){
				this.currentKey = (this.currentKey+1)%keyframes.length;
				this.frame = keyframes[this.currentKey].getStartTime();
			}
		}
		else{
			firstKeyFrame = this.lastFrame;
			secondKeyFrame = keyframes[this.currentKey];
			secondKeyFrame.setStartTime(this.lastFrame.getStartTime()+20);
			this.fixCounter++;
			//Update
			if(this.fixCounter >= 20){
				this.frame = 0;
				this.fixCounter = 0;
				this.transitionFixed = true;
				System.out.println(this.transitionFixed);
				secondKeyFrame.setStartTime(0);
			}
			this.frame++;
		}
		this.currenObjectsToDraw = firstKeyFrame.getObjects().length;
		//Interpolate
		this.interpolateBones(firstKeyFrame, secondKeyFrame, xOffset, yOffset);		
		this.interpolateObjects(firstKeyFrame, secondKeyFrame, xOffset, yOffset);	
	}
	
	private void interpolateObjects(SpriterKeyFrame firstFrame, SpriterKeyFrame secondFrame, float xOffset, float yOffset) {
		float[] newstuff;
		for (int i = 0; i < this.currenObjectsToDraw; i++) {

			SpriterObject obj1 = firstFrame.getObjects()[i];
			DrawInstruction dI = this.instructions[i];

			if (!obj1.isTransientObject()) {
					SpriterObject obj2 = null;
					boolean found = false;
					for(int j = 0; j < secondFrame.getObjects().length && !found; j++){
						obj2 = secondFrame.getObjects()[j];
						found = secondFrame.getObjects()[j].getTimeline() == obj1.getTimeline();
					}
					float x=obj1.getX(),y=obj1.getY(),scaleX=obj1.getScaleX(),scaleY=obj1.getScaleY(),rotation=obj1.getAngle(),alpha=obj1.getAlpha();
					if(obj2 != null){
						x = SpriterCalculator.calculateInterpolation(obj1.getX(), obj2.getX(), firstFrame.getStartTime(),
								secondFrame.getStartTime(),	this.frame);
						y = SpriterCalculator.calculateInterpolation(obj1.getY(), obj2.getY(), firstFrame.getStartTime(),
								secondFrame.getStartTime(),	this.frame);
		
						scaleX = SpriterCalculator.calculateInterpolation(obj1.getScaleX(), obj2.getScaleX(), firstFrame.getStartTime(),
								secondFrame.getStartTime(), this.frame);
		
						scaleY = SpriterCalculator.calculateInterpolation(obj1.getScaleY(), obj2.getScaleY(), firstFrame.getStartTime(),
								secondFrame.getStartTime(), this.frame);
		
						rotation = SpriterCalculator.calculateAngleInterpolation(obj1.getAngle(), obj2.getAngle(), firstFrame.getStartTime(),
								secondFrame.getStartTime(),	this.frame);
						alpha = SpriterCalculator.calculateInterpolation(obj1.getAlpha(), obj2.getAlpha(), firstFrame.getStartTime(),
								secondFrame.getStartTime(), this.frame);
					}
					if (obj1.getParent() != null) {					
						rotation += tempBones[obj1.getParent()].getAngle();
						scaleX *= tempBones[obj1.getParent()].getScaleX();
						scaleY *= tempBones[obj1.getParent()].getScaleY();
						
						newstuff = SpriterCalculator.rotatePoint(tempBones[obj1.getParent()], x, y);
						x = newstuff[0];
						y = newstuff[1];
	
					}
					else{
						rotation += this.angle;
						scaleX *= this.scale;
						scaleY *= this.scale;
						newstuff = SpriterCalculator.rotatePoint(this.rootParent, x, y);
						x = newstuff[0];
						y = newstuff[1];
					}
					
					dI.ref.folder = obj1.getFolder();
					dI.ref.file = obj1.getFile();
					dI.x =  x*this.flipX+xOffset;
					dI.y =  y*this.flipY+yOffset;
					dI.scaleX =  scaleX*this.flipX;
					dI.scaleY =  scaleY*this.flipY;
					dI.pivotX =  obj1.getPivotX();
					dI.pivotY = obj1.getPivotY();
					dI.angle = rotation*this.flipX*this.flipY;
					dI.alpha = alpha;
					
			}
			else
			{
				dI.ref.folder = obj1.getFolder();
				dI.ref.file = obj1.getFile();
				dI.pivotX = obj1.getPivotX();
				dI.pivotY = obj1.getPivotY();
				dI.scaleX = obj1.getScaleX()*this.scale;
				dI.scaleY = obj1.getScaleY()*this.scale;
				dI.angle = obj1.getAngle()+this.angle;
				dI.alpha = obj1.getAlpha();
				newstuff = SpriterCalculator.rotatePoint(this.rootParent, dI.x, dI.y);
				dI.x = newstuff[0];
				dI.y = newstuff[1];
				dI.x = dI.x*this.flipX+xOffset;
				dI.y = dI.y*this.flipY+yOffset;
			}
		}
	}
	
	/**
	 * Interpolates the bones for this animation.
	 * @param firstFrame first keyframe
	 * @param secondFrame second keyframe
	 * @param currentAnimationTime
	 * @param key2StartTime
	 * @return interpolated SpriterBone array
	 */
	private void interpolateBones(SpriterKeyFrame firstFrame, SpriterKeyFrame secondFrame, float xOffset, float yOffset){
		for (int i = 0; i < firstFrame.getBones().length; i++) {

			SpriterBone bone1 = firstFrame.getBones()[i];
			this.tempBones[i].setName(bone1.getName());
			this.moddedBones[i].setName(bone1.getName());
			SpriterBone bone2 = null;
			boolean found = false;
			for(int j = 0; j < secondFrame.getBones().length && !found; j++){//Get the right bone to interpolate with
				if(secondFrame.getBones()[j].getTimeline() == bone1.getTimeline()){
					bone2 = secondFrame.getBones()[j];
					found = true;
				}
			}
			float x=bone1.getX(),y=bone1.getY(),scaleX=bone1.getScaleX(),scaleY=bone1.getScaleY(),rotation=bone1.getAngle();
			if(bone2 != null){
				x = SpriterCalculator.calculateInterpolation(bone1.getX(), bone2.getX(), firstFrame.getStartTime(), secondFrame.getStartTime(), this.frame);
				y = SpriterCalculator.calculateInterpolation(bone1.getY(), bone2.getY(), firstFrame.getStartTime(), secondFrame.getStartTime(), this.frame);
				scaleX = SpriterCalculator.calculateInterpolation(bone1.getScaleX(), bone2.getScaleX(), firstFrame.getStartTime(), secondFrame.getStartTime(), this.frame);
				scaleY = SpriterCalculator.calculateInterpolation(bone1.getScaleY(), bone2.getScaleY(), firstFrame.getStartTime(), secondFrame.getStartTime(), this.frame);
				rotation = SpriterCalculator.calculateAngleInterpolation(bone1.getAngle(), bone2.getAngle(), firstFrame.getStartTime(), secondFrame.getStartTime(), this.frame);
			}
			rotation += this.moddedBones[i].getAngle();
			scaleX *= this.moddedBones[i].getScaleX();
			scaleY *= this.moddedBones[i].getScaleY();
			this.tempBones[i].setAngle((this.tempBones[i].getSpin() == -1) ? 360-rotation: rotation);
			this.tempBones[i].setId(bone1.getId());
			this.tempBones[i].setParent(bone1.getParent());
			this.tempBones[i].setScaleX(scaleX);
			this.tempBones[i].setScaleY(scaleY);
			this.tempBones[i].setX(x);
			this.tempBones[i].setY(y);
			if (this.tempBones[i].getParent() != null) {
				this.tempBones[i].setAngle(this.tempBones[i].getAngle() + tempBones[this.tempBones[i].getParent()].getAngle());
				this.tempBones[i].setScaleX(this.tempBones[i].getScaleX() * tempBones[this.tempBones[i].getParent()].getScaleX());
				this.tempBones[i].setScaleY(this.tempBones[i].getScaleY() * tempBones[this.tempBones[i].getParent()].getScaleY());
				float[] newstuff = SpriterCalculator.rotatePoint(tempBones[this.tempBones[i].getParent()], this.tempBones[i].getX(), this.tempBones[i].getY());
				this.tempBones[i].setX(newstuff[0]);
				this.tempBones[i].setY(newstuff[1]);
			}
			else{
				this.tempBones[i].setAngle(this.tempBones[i].getAngle() + this.rootParent.getAngle());
				this.tempBones[i].setScaleX(this.tempBones[i].getScaleX() * this.rootParent.getScaleX());
				this.tempBones[i].setScaleY(this.tempBones[i].getScaleY() * this.rootParent.getScaleY());
				float[] newstuff = SpriterCalculator.rotatePoint(this.rootParent, this.tempBones[i].getX(), this.tempBones[i].getY());
				this.tempBones[i].setX(newstuff[0]);
				this.tempBones[i].setY(newstuff[1]);
			}
			this.moddedBones[i].setX(this.tempBones[i].getX()+xOffset);
			this.moddedBones[i].setY(this.tempBones[i].getY()+yOffset);
		}
	} 
	
	/**
	 * Sets the current animation index for this player.
	 * @param animationIndex
	 */
	public void setAnimatioIndex(int animationIndex){
		if(this.animationIndex != animationIndex && this.transitionFixed){
			//this.frame = 0;
			SpriterKeyFrame curFrame = this.keyframes.get(this.animationIndex)[this.currentKey];
			this.lastFrame.setStartTime(this.frame);
			this.transitionFixed = false;
			for(int i = 0; i < this.lastFrame.getBones().length; i++){
				SpriterBone bone = this.lastFrame.getBones()[i];
				bone.setX(this.tempBones[i].getX());
				bone.setY(this.tempBones[i].getY());
				if(this.tempBones[i].getParent() != null){
					bone.setAngle(this.tempBones[i].getAngle()- this.tempBones[this.tempBones[i].getParent()].getAngle());
					bone.setScaleX(this.tempBones[i].getScaleX() / tempBones[this.tempBones[i].getParent()].getScaleX());
					bone.setScaleY(this.tempBones[i].getScaleY() / tempBones[this.tempBones[i].getParent()].getScaleY());
				}
				else{
					bone.setAngle(this.tempBones[i].getAngle()- this.rootParent.getAngle());
					bone.setScaleX(this.tempBones[i].getScaleX() / this.rootParent.getScaleX());
					bone.setScaleY(this.tempBones[i].getScaleY() / this.rootParent.getScaleY());
				}
				bone.setId(this.tempBones[i].getId());
				bone.setName(this.tempBones[i].getName());
				bone.setParent(this.tempBones[i].getParent());
				bone.setSpin(this.tempBones[i].getSpin());
				bone.setTimeline(this.tempBones[i].getTimeline());
			}
			for(int i = 0; i < this.currenObjectsToDraw; i++){
				SpriterObject obj = this.lastFrame.getObjects()[i];
				obj.setAlpha(this.instructions[i].alpha);
				obj.setAngle(this.instructions[i].angle*this.flipX*this.flipY);
				obj.setFile(this.instructions[i].ref.file);
				obj.setFolder(this.instructions[i].ref.folder);
				obj.setX((this.instructions[i].x-this.xOffset)*this.flipX);
				obj.setY((this.instructions[i].y-this.yOffset)*this.flipY);
				obj.setPivotX(this.instructions[i].pivotX);
				obj.setPivotY(this.instructions[i].pivotY);
				obj.setScale_x(this.instructions[i].scaleX*this.flipX);
				obj.setScale_y(this.instructions[i].scaleY*this.flipY);
				obj.setSpin(curFrame.getObjects()[i].getSpin());
				obj.setzIndex(curFrame.getObjects()[i].getZIndex());
				obj.setTimeline(curFrame.getObjects()[i].getTimeline());
				obj.setId(curFrame.getObjects()[i].getId());
			}
			this.currentKey = 0;
			this.animationIndex = animationIndex;
			this.animation = this.spriterData.getEntity().get(0).getAnimation().get(animationIndex);
		}
	}
	
	public int getAnimationIndexByName(String name){
		List<Animation> anims = this.spriterData.getEntity().get(0).getAnimation();
		for(Animation anim: anims)
			if(anim.getName().equals(name)) return anim.getId();
		return 0;
	}
	
	public int getBoneIndexByName(String name){
		for(int i = 0; i < this.moddedBones.length; i++)
			if(name.equals(this.moddedBones[i].getName())) return i;
		return 0;
	}
	
	public void setBoneAngle(int index, float angle){
		this.moddedBones[index].setAngle(angle);
	}
	
	public void setBoneScaleX(int index, float scaleX){
		this.moddedBones[index].setScaleX(scaleX);
	}
	
	public void setBoneScaleY(int index, float scaleY){
		this.moddedBones[index].setScaleX(scaleY);
	}
	
	public float getBoneAngle(int index){
		return this.moddedBones[index].getAngle();
	}
	
	public float getBoneScaleX(int index){
		return this.moddedBones[index].getScaleX();
	}
	
	public float getBoneScaleY(int index){
		return this.moddedBones[index].getScaleY();
	}
	
	public float getBoneX(int index){
		return this.moddedBones[index].getX();
	}
	
	public float getBoneY(int index){
		return this.moddedBones[index].getY();
	}
	
	/**
	 * @return current animation index
	 */
	public int getAnimationIndex(){
		return this.animationIndex;
	}
	
	
	/**
	 * Returns the current DrawInstruction array
	 * @param animationIndex
	 * @return
	 */
	public DrawInstruction[] getDrawInstructions(){
		return this.instructions;
	}

	/**
	 * @return the spriterData
	 */
	public SpriterData getSpriterData() {
		return spriterData;
	}

	/**
	 * @param spriterData the spriterData to set
	 */
	public void setSpriterData(SpriterData spriterData) {
		this.spriterData = spriterData;
	}

	/**
	 * @return the frame
	 */
	public long getFrame() {
		return frame;
	}

	/**
	 * @param frame the frame to set
	 */
	public void setFrame(long frame) {
		this.frame = frame;
	}

	/**
	 * @return the frameSpeed
	 */
	public int getFrameSpeed() {
		return frameSpeed;
	}

	/**
	 * @param frameSpeed the frameSpeed to set
	 */
	public void setFrameSpeed(int frameSpeed) {
		this.frameSpeed = frameSpeed;
	}

	
	/**
	 * @return the anim
	 */
	public Animation getAnimation() {
		return animation;
	}

	
	/**
	 * @param anim the anim to set
	 */
	public void setAnimation(Animation anim) {
		this.animation = anim;
	}
	
	public int getFlipX(){
		return this.flipX;
	}
	
	public void flipX(){
		this.flipX *=-1;
	}
	
	public void flipY(){
		this.flipY *=-1;
	}
	
	public void setAngle(float angle){
		this.rootParent.setAngle(this.angle);
		this.angle = angle;
	}
	
	public float getAngle(){
		return this.angle;
	}

	public float getFlipY() {
		return this.flipY;
	}

	/**
	 * @return the scaleX
	 */
	public float getScale() {
		return scale;
	}

	/**
	 * @param scaleX the scaleX to set
	 */
	public void setScale(float scale) {
		this.scale = scale;
		this.rootParent.setScaleX(this.scale);
		this.rootParent.setScaleY(this.scale);
	}
	
	public void setPivot(float pivotX, float pivotY){
		this.rootParent.setX(pivotX);
		this.rootParent.setY(pivotY);
	}
	
	public float getPivotX(){
		return this.rootParent.getX();
	}
	
	public float getPivotY(){
		return this.rootParent.getY();
	}
}
