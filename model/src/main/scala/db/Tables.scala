package db
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.driver.PostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import com.github.tototoshi.slick.PostgresJodaSupport._
  import org.joda.time.DateTime
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema = Block.schema ++ BlockTx.schema ++ PlayEvolutions.schema ++ Prediction.schema ++ Tx.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Block
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param height Database column height SqlType(int4)
   *  @param hash Database column hash SqlType(bpchar), Length(64,false)
   *  @param parentHash Database column parent_hash SqlType(bpchar), Length(64,false)
   *  @param createDate Database column create_date SqlType(timestamp)
   *  @param minFee Database column min_fee SqlType(int8)
   *  @param txCount Database column tx_count SqlType(int4)
   *  @param knownCount Database column known_count SqlType(int4)
   *  @param size Database column size SqlType(int4) */
  case class BlockRow(id: Int, height: Int, hash: String, parentHash: String, createDate: DateTime, minFee: Long, txCount: Int, knownCount: Int, size: Int)
  /** GetResult implicit for fetching BlockRow objects using plain SQL queries */
  implicit def GetResultBlockRow(implicit e0: GR[Int], e1: GR[String], e2: GR[DateTime], e3: GR[Long]): GR[BlockRow] = GR{
    prs => import prs._
    BlockRow.tupled((<<[Int], <<[Int], <<[String], <<[String], <<[DateTime], <<[Long], <<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table block. Objects of this class serve as prototypes for rows in queries. */
  class Block(_tableTag: Tag) extends Table[BlockRow](_tableTag, "block") {
    def * = (id, height, hash, parentHash, createDate, minFee, txCount, knownCount, size) <> (BlockRow.tupled, BlockRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(height), Rep.Some(hash), Rep.Some(parentHash), Rep.Some(createDate), Rep.Some(minFee), Rep.Some(txCount), Rep.Some(knownCount), Rep.Some(size)).shaped.<>({r=>import r._; _1.map(_=> BlockRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column height SqlType(int4) */
    val height: Rep[Int] = column[Int]("height")
    /** Database column hash SqlType(bpchar), Length(64,false) */
    val hash: Rep[String] = column[String]("hash", O.Length(64,varying=false))
    /** Database column parent_hash SqlType(bpchar), Length(64,false) */
    val parentHash: Rep[String] = column[String]("parent_hash", O.Length(64,varying=false))
    /** Database column create_date SqlType(timestamp) */
    val createDate: Rep[DateTime] = column[DateTime]("create_date")
    /** Database column min_fee SqlType(int8) */
    val minFee: Rep[Long] = column[Long]("min_fee")
    /** Database column tx_count SqlType(int4) */
    val txCount: Rep[Int] = column[Int]("tx_count")
    /** Database column known_count SqlType(int4) */
    val knownCount: Rep[Int] = column[Int]("known_count")
    /** Database column size SqlType(int4) */
    val size: Rep[Int] = column[Int]("size")

    /** Uniqueness Index over (hash) (database name block_hash_idx) */
    val index1 = index("block_hash_idx", hash, unique=true)
    /** Index over (height) (database name block_height_index) */
    val index2 = index("block_height_index", height)
  }
  /** Collection-like TableQuery object for table Block */
  lazy val Block = new TableQuery(tag => new Block(tag))

  /** Entity class storing rows of table BlockTx
   *  @param blockId Database column block_id SqlType(int4)
   *  @param txId Database column tx_id SqlType(int4) */
  case class BlockTxRow(blockId: Int, txId: Int)
  /** GetResult implicit for fetching BlockTxRow objects using plain SQL queries */
  implicit def GetResultBlockTxRow(implicit e0: GR[Int]): GR[BlockTxRow] = GR{
    prs => import prs._
    BlockTxRow.tupled((<<[Int], <<[Int]))
  }
  /** Table description of table block_tx. Objects of this class serve as prototypes for rows in queries. */
  class BlockTx(_tableTag: Tag) extends Table[BlockTxRow](_tableTag, "block_tx") {
    def * = (blockId, txId) <> (BlockTxRow.tupled, BlockTxRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(blockId), Rep.Some(txId)).shaped.<>({r=>import r._; _1.map(_=> BlockTxRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column block_id SqlType(int4) */
    val blockId: Rep[Int] = column[Int]("block_id")
    /** Database column tx_id SqlType(int4) */
    val txId: Rep[Int] = column[Int]("tx_id")

    /** Primary key of BlockTx (database name block_tx_pkey) */
    val pk = primaryKey("block_tx_pkey", (blockId, txId))

    /** Foreign key referencing Block (database name block_tx_block_id_fkey) */
    lazy val blockFk = foreignKey("block_tx_block_id_fkey", blockId, Block)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    /** Foreign key referencing Tx (database name block_tx_tx_id_fkey) */
    lazy val txFk = foreignKey("block_tx_tx_id_fkey", txId, Tx)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table BlockTx */
  lazy val BlockTx = new TableQuery(tag => new BlockTx(tag))

  /** Entity class storing rows of table PlayEvolutions
   *  @param id Database column id SqlType(int4), PrimaryKey
   *  @param hash Database column hash SqlType(varchar), Length(255,true)
   *  @param appliedAt Database column applied_at SqlType(timestamp)
   *  @param applyScript Database column apply_script SqlType(text), Default(None)
   *  @param revertScript Database column revert_script SqlType(text), Default(None)
   *  @param state Database column state SqlType(varchar), Length(255,true), Default(None)
   *  @param lastProblem Database column last_problem SqlType(text), Default(None) */
  case class PlayEvolutionsRow(id: Int, hash: String, appliedAt: DateTime, applyScript: Option[String] = None, revertScript: Option[String] = None, state: Option[String] = None, lastProblem: Option[String] = None)
  /** GetResult implicit for fetching PlayEvolutionsRow objects using plain SQL queries */
  implicit def GetResultPlayEvolutionsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[DateTime], e3: GR[Option[String]]): GR[PlayEvolutionsRow] = GR{
    prs => import prs._
    PlayEvolutionsRow.tupled((<<[Int], <<[String], <<[DateTime], <<?[String], <<?[String], <<?[String], <<?[String]))
  }
  /** Table description of table play_evolutions. Objects of this class serve as prototypes for rows in queries. */
  class PlayEvolutions(_tableTag: Tag) extends Table[PlayEvolutionsRow](_tableTag, "play_evolutions") {
    def * = (id, hash, appliedAt, applyScript, revertScript, state, lastProblem) <> (PlayEvolutionsRow.tupled, PlayEvolutionsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(hash), Rep.Some(appliedAt), applyScript, revertScript, state, lastProblem).shaped.<>({r=>import r._; _1.map(_=> PlayEvolutionsRow.tupled((_1.get, _2.get, _3.get, _4, _5, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column hash SqlType(varchar), Length(255,true) */
    val hash: Rep[String] = column[String]("hash", O.Length(255,varying=true))
    /** Database column applied_at SqlType(timestamp) */
    val appliedAt: Rep[DateTime] = column[DateTime]("applied_at")
    /** Database column apply_script SqlType(text), Default(None) */
    val applyScript: Rep[Option[String]] = column[Option[String]]("apply_script", O.Default(None))
    /** Database column revert_script SqlType(text), Default(None) */
    val revertScript: Rep[Option[String]] = column[Option[String]]("revert_script", O.Default(None))
    /** Database column state SqlType(varchar), Length(255,true), Default(None) */
    val state: Rep[Option[String]] = column[Option[String]]("state", O.Length(255,varying=true), O.Default(None))
    /** Database column last_problem SqlType(text), Default(None) */
    val lastProblem: Rep[Option[String]] = column[Option[String]]("last_problem", O.Default(None))
  }
  /** Collection-like TableQuery object for table PlayEvolutions */
  lazy val PlayEvolutions = new TableQuery(tag => new PlayEvolutions(tag))

  /** Entity class storing rows of table Prediction
   *  @param hash Database column hash SqlType(bpchar), PrimaryKey, Length(64,false)
   *  @param height Database column height SqlType(int4)
   *  @param delayMin Database column delay_min SqlType(int4)
   *  @param delayMax Database column delay_max SqlType(int4)
   *  @param minutesMin Database column minutes_min SqlType(int4)
   *  @param minutesMax Database column minutes_max SqlType(int4)
   *  @param createDate Database column create_date SqlType(timestamp) */
  case class PredictionRow(hash: String, height: Int, delayMin: Int, delayMax: Int, minutesMin: Int, minutesMax: Int, createDate: DateTime)
  /** GetResult implicit for fetching PredictionRow objects using plain SQL queries */
  implicit def GetResultPredictionRow(implicit e0: GR[String], e1: GR[Int], e2: GR[DateTime]): GR[PredictionRow] = GR{
    prs => import prs._
    PredictionRow.tupled((<<[String], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int], <<[DateTime]))
  }
  /** Table description of table prediction. Objects of this class serve as prototypes for rows in queries. */
  class Prediction(_tableTag: Tag) extends Table[PredictionRow](_tableTag, "prediction") {
    def * = (hash, height, delayMin, delayMax, minutesMin, minutesMax, createDate) <> (PredictionRow.tupled, PredictionRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(hash), Rep.Some(height), Rep.Some(delayMin), Rep.Some(delayMax), Rep.Some(minutesMin), Rep.Some(minutesMax), Rep.Some(createDate)).shaped.<>({r=>import r._; _1.map(_=> PredictionRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column hash SqlType(bpchar), PrimaryKey, Length(64,false) */
    val hash: Rep[String] = column[String]("hash", O.PrimaryKey, O.Length(64,varying=false))
    /** Database column height SqlType(int4) */
    val height: Rep[Int] = column[Int]("height")
    /** Database column delay_min SqlType(int4) */
    val delayMin: Rep[Int] = column[Int]("delay_min")
    /** Database column delay_max SqlType(int4) */
    val delayMax: Rep[Int] = column[Int]("delay_max")
    /** Database column minutes_min SqlType(int4) */
    val minutesMin: Rep[Int] = column[Int]("minutes_min")
    /** Database column minutes_max SqlType(int4) */
    val minutesMax: Rep[Int] = column[Int]("minutes_max")
    /** Database column create_date SqlType(timestamp) */
    val createDate: Rep[DateTime] = column[DateTime]("create_date")
  }
  /** Collection-like TableQuery object for table Prediction */
  lazy val Prediction = new TableQuery(tag => new Prediction(tag))

  /** Entity class storing rows of table Tx
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param hash Database column hash SqlType(bpchar), Length(64,false)
   *  @param fee Database column fee SqlType(int8)
   *  @param absFee Database column abs_fee SqlType(int8)
   *  @param size Database column size SqlType(int4)
   *  @param createDate Database column create_date SqlType(timestamp)
   *  @param mineDate Database column mine_date SqlType(timestamp), Default(None) */
  case class TxRow(id: Int, hash: String, fee: Long, absFee: Long, size: Int, createDate: DateTime, mineDate: Option[DateTime] = None)
  /** GetResult implicit for fetching TxRow objects using plain SQL queries */
  implicit def GetResultTxRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Long], e3: GR[DateTime], e4: GR[Option[DateTime]]): GR[TxRow] = GR{
    prs => import prs._
    TxRow.tupled((<<[Int], <<[String], <<[Long], <<[Long], <<[Int], <<[DateTime], <<?[DateTime]))
  }
  /** Table description of table tx. Objects of this class serve as prototypes for rows in queries. */
  class Tx(_tableTag: Tag) extends Table[TxRow](_tableTag, "tx") {
    def * = (id, hash, fee, absFee, size, createDate, mineDate) <> (TxRow.tupled, TxRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(hash), Rep.Some(fee), Rep.Some(absFee), Rep.Some(size), Rep.Some(createDate), mineDate).shaped.<>({r=>import r._; _1.map(_=> TxRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column hash SqlType(bpchar), Length(64,false) */
    val hash: Rep[String] = column[String]("hash", O.Length(64,varying=false))
    /** Database column fee SqlType(int8) */
    val fee: Rep[Long] = column[Long]("fee")
    /** Database column abs_fee SqlType(int8) */
    val absFee: Rep[Long] = column[Long]("abs_fee")
    /** Database column size SqlType(int4) */
    val size: Rep[Int] = column[Int]("size")
    /** Database column create_date SqlType(timestamp) */
    val createDate: Rep[DateTime] = column[DateTime]("create_date")
    /** Database column mine_date SqlType(timestamp), Default(None) */
    val mineDate: Rep[Option[DateTime]] = column[Option[DateTime]]("mine_date", O.Default(None))

    /** Uniqueness Index over (hash) (database name tx_hash_key) */
    val index1 = index("tx_hash_key", hash, unique=true)
    /** Index over (mineDate) (database name tx_mine_date_index) */
    val index2 = index("tx_mine_date_index", mineDate)
  }
  /** Collection-like TableQuery object for table Tx */
  lazy val Tx = new TableQuery(tag => new Tx(tag))
}
