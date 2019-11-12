package com.wavesplatform.history

import com.wavesplatform.block.{Block, MicroBlock}
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.database
import com.wavesplatform.database.DBExt
import com.wavesplatform.state.{Blockchain, Height}
import org.iq80.leveldb.DB

trait History {
  def loadBlockBytes(id: ByteStr): Option[Array[Byte]]
  def loadMicroBlockBytes(id: ByteStr): Option[Array[Byte]]
  def blockIdsAfter(candidates: Seq[ByteStr], count: Int): Seq[ByteStr]
}

object History {
  def apply(blockchain: Blockchain, liquidBlock: ByteStr => Option[Block], microBlock: ByteStr => Option[MicroBlock], db: DB): History = new History {
    override def loadBlockBytes(id: ByteStr): Option[Array[Byte]] = {
      liquidBlock(id)
        .map(_.bytes())
        .orElse(blockchain.heightOf(id).flatMap { h =>
          db.readOnly { ro =>
            database.loadBlock(Height(h), ro).map(_.bytes())
          }
        })
    }

    override def loadMicroBlockBytes(id: ByteStr): Option[Array[Byte]] =
      microBlock(id).map(_.bytes())

    override def blockIdsAfter(candidates: Seq[ByteStr], count: Int): Seq[ByteStr] =
      candidates.view.flatMap(blockchain.heightOf).headOption.fold[Seq[ByteStr]](Seq.empty) { firstCommonHeight =>
        (firstCommonHeight to firstCommonHeight + count).flatMap(blockchain.blockId)
      }
  }
}
