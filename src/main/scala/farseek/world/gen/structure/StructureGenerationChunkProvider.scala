package farseek.world.gen.structure

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import farseek.util.Reflection._
import farseek.util._
import net.minecraft.world._
import net.minecraft.world.gen.structure.MapGenStructure
import net.minecraftforge.common.MinecraftForge._
import net.minecraftforge.event.world.WorldEvent
import scala.collection.mutable

/** A chunk provider for [[Structure]]s that contains a copy of a world's chunk
  * generator and creates chunks _without_ structures.
  *
  * These chunks are used for structures being generated to query the terrain in
  * their range without triggering recursion by generating more of themselves.
  * This enables Farseek structures to be terrain-aware in a way that vanilla
  * structures cannot be, but also means that structures cannot know of others
  * that can appear in their range. The implementor is responsible for either
  * preventing these situations (ex.: by partitioning the world into distinct
  * areas for generation, as is done for Streams) or handling collisions
  * gracefully.
  *
  * @author
  *   delvr
  */
class StructureGenerationChunkProvider(val worldProvider: WorldProvider)
    extends Logging {

  debug(
    s"Creating structure generation chunk provider for world $worldProvider"
  )

  val generator = worldProvider.createChunkGenerator

  classFieldValues[MapGenStructure](generator)
    .foreach(_.range = -1) // Disable structure generators
  EVENT_BUS.register(this)

  def generateChunk(xChunk: Int, zChunk: Int) = {
    val chunk = generator.provideChunk(xChunk, zChunk)
    chunk.isTerrainPopulated = true
    chunk
  }

  @SubscribeEvent def onWorldUnload(event: WorldEvent.Unload) {
    if (event.world.provider == this.worldProvider) {
      debug(
        s"Removing structure generation chunk provider for world $worldProvider"
      )
      EVENT_BUS.unregister(this)
      StructureGenerationChunkProvider.remove(event.world.provider)
    }
  }
}

/** Companion object for [[StructureGenerationChunkProvider]]s, that maintains a
  * mapping of [[WorldProvider]]s to `StructureGenerationChunkProvider`s.
  * @author
  *   delvr
  */
object StructureGenerationChunkProvider extends Logging {

  private val providers =
    mutable.Map[WorldProvider, StructureGenerationChunkProvider]()

  def apply(worldProvider: WorldProvider) = providers.getOrElseUpdate(
    worldProvider,
    new StructureGenerationChunkProvider(worldProvider)
  )

  def remove(worldProvider: WorldProvider) = providers.remove(worldProvider)
}
