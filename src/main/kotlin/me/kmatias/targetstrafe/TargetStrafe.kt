package me.kmatias.targetstrafe

import com.lambda.client.event.SafeClientEvent
import com.lambda.client.event.events.PlayerMoveEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.manager.managers.CombatManager
import com.lambda.client.module.Category
import com.lambda.client.module.modules.combat.KillAura
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.KeyboardUtils
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.LambdaTessellator
import com.lambda.client.util.math.RotationUtils.getRotationToEntity
import com.lambda.client.util.threads.safeListener
import com.lambda.event.listener.listener
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.MobEffects
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.GL_LINE_STRIP
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TargetStrafe : PluginModule(
    name = "TargetStrafe",
    description = "Strafes around a target in a circle",
    category = Category.COMBAT,
    pluginMain = Main
) {
    private val autoJump by setting("AutoJump", true)
    private val distanceSetting by setting("PreferredDistance", 1.0, 0.0..6.0, 0.1)
    private val maxDistance by setting("MaxDistance", 10f, 1f..32f, 0.5f)
    private val turnAmount by setting("TurnAmount", 5f, 1f..90f, 0.5f)
    private val hSpeed by setting("HSpeed", 0.2873f, 0.001f..10.0f, 0.0001f)

    private val needsAura by setting("NeedsAura", true)
    private val antiStuck by setting("AntiStuck", true)

//    private val renderCircle by setting("RenderCircle", true, { false })
//    private val renderThickness by setting("RenderThickness", 2f, 0.5f..8f, 0.5f, { renderCircle })
//    private val distanceColor by setting("DistanceColor", ColorHolder(255, 240, 246), false)
//    private val playerDistanceColor by setting("PlayerDistanceColor", ColorHolder(214, 0, 0), false)

    private var direction = 1
    private var currentDistance = 0.0
    private var currentTargetVec: Vec3d? = null
    private var strafing = false

    init {

        safeListener<PlayerMoveEvent> { event ->
            if (player.collidedHorizontally && antiStuck) {
                switchDirection()
            }

            if (canStrafe()) {
                CombatManager.target?.let { entity ->
                    doStrafeAtSpeed(event, getRotationToEntity(entity).x, entity.positionVector)
                    currentTargetVec = entity.positionVector.add(player.lookVec)
                }
                strafing = true
            } else {
                strafing = false
            }
        }

        // ToDo: Fix plugin API bug
//        listener<RenderWorldEvent> {
//            if (strafing && renderCircle) {
//                currentTargetVec?.let {
//                    drawCircle(it, distanceSetting, distanceColor, 360)
//                    drawCircle(it, currentDistance, playerDistanceColor, 360)
//                }
//            }
//        }
    }

    private fun SafeClientEvent.doStrafeAtSpeed(event: PlayerMoveEvent, rotation: Float, target: Vec3d) {
        var playerSpeed = hSpeed
        var jumpVelocity = 0.405

        var rotationYaw = rotation + (90f * direction)

        val disX = player.posX - target.x
        val disZ = player.posZ - target.z
        val distance = sqrt(disX * disX + disZ * disZ)

        // ToDo: make it always move to the next point in the circle instead of jitter
        if (distance < maxDistance) {
            if (distance > distanceSetting) {
                rotationYaw -= turnAmount * direction
            } else if (distance < distanceSetting) {
                rotationYaw += turnAmount * direction
            }
        } else {
            rotationYaw = rotation
        }

        currentDistance = distance


        // jump boost
        if (player.isPotionActive(MobEffects.JUMP_BOOST)) {
            jumpVelocity *= player.getActivePotionEffect(MobEffects.JUMP_BOOST)!!.amplifier
        }

        // speed
        if (player.isPotionActive(MobEffects.SPEED)) {
            playerSpeed *= 1.0f + 0.2f * (player.getActivePotionEffect(MobEffects.SPEED)!!.amplifier + 1)
        }

        event.x = playerSpeed * cos(Math.toRadians((rotationYaw + 90.0f).toDouble()))
        event.z = playerSpeed * sin(Math.toRadians((rotationYaw + 90.0f).toDouble()))


        if (autoJump && player.onGround) {
            player.jump()
        }
    }

    private fun canStrafe(): Boolean =
        (KillAura.isEnabled || !needsAura) && CombatManager.target != null

    private fun switchDirection() {
        direction = -direction
    }

//    private fun drawCircle(center: Vec3d, radius: Double, color: ColorHolder, precision: Int) {
//        val linesToDraw = ArrayList<Pair<Vec3d, Vec3d>>()
//        val magic = precision / 360
//        var lastPos = center.add(0.0, 0.0, radius)
//
//        for (i in 0..precision) {
//            val yaw = i * magic
//            val x = radius * cos(Math.toRadians((yaw + 90.0f).toDouble()))
//            val z = radius * sin(Math.toRadians((yaw + 90.0f).toDouble()))
//
//            val newPos = center.add(x, 0.0, z)
//            linesToDraw.add(Pair(lastPos, newPos))
//            lastPos = newPos
//        }
//
//        val buffer = LambdaTessellator.buffer
//        GlStateManager.glLineWidth(renderThickness)
//        LambdaTessellator.begin(GL_LINE_STRIP)
//        linesToDraw.forEach { pair ->
//            buffer.pos(pair.first.x, center.y, pair.first.z).color(color.r, color.g, color.b, color.a).endVertex()
//            buffer.pos(pair.second.x, center.y, pair.second.z).color(color.r, color.g, color.b, color.a).endVertex()
//        }
//        LambdaTessellator.render()
//    }
}