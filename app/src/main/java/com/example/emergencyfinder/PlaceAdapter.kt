
package com.example.emergencyfinder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class PlaceAdapter(
    private val context: Context,
    private val places: List<Place>,
    private val onFavouriteClick: (Place) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val name: TextView =
            itemView.findViewById(R.id.txtPlaceName)

        val category: TextView =
            itemView.findViewById(R.id.txtPlaceCategory)

        val distance: TextView =
            itemView.findViewById(R.id.txtPlaceDistance)

        val directions: Button =
            itemView.findViewById(R.id.btnDirections)

        val call: Button =
            itemView.findViewById(R.id.btnCall)

        val favourite: Button =
            itemView.findViewById(R.id.btnFavourite)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlaceViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)

        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PlaceViewHolder,
        position: Int
    ) {

        val place = places[position]

        holder.name.text = place.name
        holder.category.text =
            place.category.replaceFirstChar { it.uppercase() }

        holder.distance.text = String.format(
            Locale.getDefault(),
            "%.2f km away",
            place.distanceKm
        )

        holder.directions.setOnClickListener {

            val uri = Uri.parse(
                "geo:${place.latitude},${place.longitude}" +
                        "?q=${place.latitude},${place.longitude}" +
                        "(${Uri.encode(place.name)})"
            )

            val intent = Intent(Intent.ACTION_VIEW, uri)

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "No maps application found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (place.phone.isNullOrBlank()) {
            holder.call.visibility = View.GONE
        } else {
            holder.call.visibility = View.VISIBLE

            holder.call.setOnClickListener {

                val intent = Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:${place.phone}")
                )

                context.startActivity(intent)
            }
        }

        holder.favourite.setOnClickListener {
            onFavouriteClick(place)
        }
    }

    override fun getItemCount(): Int = places.size
}
