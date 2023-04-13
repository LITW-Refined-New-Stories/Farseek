package farseek.core

import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex
import cpw.mods.fml.relauncher._
import farseek.util.Logging
import java.io._
import java.net.URL
import java.util.zip.ZipFile
import net.minecraft.launchwrapper._
import scala.collection.JavaConversions._
import scala.io.Source._

/** Core mod class for Farseek.
  * @see
  *   [[farseek.FarseekMod]] for non-core mod class.
  * @author
  *   delvr
  */
@SortingIndex(value =
  FMLDeobftweakerSortIndex + 100
) // So we get deobfuscated method call arguments
class FarseekCoreMod extends IFMLLoadingPlugin {

  Launch.classLoader.addTransformerExclusion(getClass.getPackage.getName)
  Launch.classLoader.addTransformerExclusion("farseek.util.Logging")
  Launch.classLoader.addTransformerExclusion("scala")

  val getSetupClass = null
  val getModContainerClass = null
  val getAccessTransformerClass = null
  val getASMTransformerClass = Array(classOf[FarseekClassTransformer].getName)

  def injectData(data: java.util.Map[String, AnyRef]) = {
    gameDir = new File(data("mcLocation").toString)
    isDev = !data("runtimeDeobfuscationEnabled").toString.toBoolean
  }
}

class FarseekClassTransformer extends IClassTransformer with Logging {

  private val ReplacementsFilepath = "META-INF/farseek_cm.cfg"

  private lazy val replacements: Map[ReplacedMethod, MethodReplacement] = {
    val allReplacements =
      (allFiles(new File(gameDir, "mods")).flatMap(methodReplacements) ++
        Launch.classLoader
          .getResources(ReplacementsFilepath)
          .flatMap(methodReplacements)).toSet
    allReplacements
      .groupBy(_._1)
      .values
      .find(_.size > 1)
      .foreach(conflicts =>
        sys.error(
          s"Found conflicting method replacements:\n  ${conflicts.mkString("\n")}"
        )
      )
    allReplacements.toMap
  }

  private def methodReplacements(
      file: File
  ): Seq[(ReplacedMethod, MethodReplacement)] = {
    if (!file.getName.endsWith(".jar")) Seq()
    else
      logged(
        file,
        using(new ZipFile(file))(zipFile =>
          Option(zipFile.getEntry(ReplacementsFilepath)).fold(
            Seq[(ReplacedMethod, MethodReplacement)]()
          )(entry => methodReplacements(zipFile.getInputStream(entry)))
        )
      )
  }

  private def methodReplacements(
      url: URL
  ): Seq[(ReplacedMethod, MethodReplacement)] =
    logged(url, methodReplacements(url.openStream))

  private def methodReplacements(
      stream: InputStream
  ): Seq[(ReplacedMethod, MethodReplacement)] =
    using(fromInputStream(stream))(
      _.getLines()
        .filter(_.takeWhile(_ != '#').trim.nonEmpty)
        .map(methodReplacement)
        .toList
    )

  private def methodReplacement(
      line: String
  ): (ReplacedMethod, MethodReplacement) = {
    val Array(replacementClass, declaringClass, devName, srgName, descriptor) =
      line.split("\\s+")
    ReplacedMethod(
      internalName(declaringClass),
      if (isDev) devName else srgName,
      descriptor
    ) ->
      MethodReplacement(internalName(replacementClass), devName)
  }

  private def logged(
      source: Any,
      replacements: Seq[(ReplacedMethod, MethodReplacement)]
  ): Seq[(ReplacedMethod, MethodReplacement)] = {
    if (replacements.nonEmpty)
      debug(
        s"Found method replacements in $source:\n  ${replacements.mkString("\n  ")}"
      )
    replacements
  }

  def transform(
      obfuscatedName: String,
      deobfuscatedName: String,
      bytecode: Array[Byte]
  ) =
    if (bytecode == null) null
    else
      new FarseekClassVisitor(
        bytecode,
        internalName(deobfuscatedName),
        replacements
      ).patch
}

case class ReplacedMethod(
    className: String,
    methodName: String,
    descriptor: String
)

case class MethodReplacement(className: String, methodName: String)
