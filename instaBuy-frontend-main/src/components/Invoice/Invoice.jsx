import { useRef } from "react";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";
import "./Invoice.css";

function formatCurrency(value) {
  return Number(value || 0).toLocaleString("en-IN");
}

function Invoice({ order }) {
  const invoiceRef = useRef(null);
  const items = order?.items || [];

  const downloadInvoice = async () => {
    if (!invoiceRef.current) return;

    const canvas = await html2canvas(invoiceRef.current, {
      scale: 2,
      useCORS: true,
      backgroundColor: "#f4f4f4",
    });

    const imgData = canvas.toDataURL("image/png");
    const pdf = new jsPDF("p", "mm", "a4");
    const pageWidth = pdf.internal.pageSize.getWidth();
    const pageHeight = pdf.internal.pageSize.getHeight();

    pdf.addImage(imgData, "PNG", 0, 0, pageWidth, pageHeight);
    pdf.save(`invoice_${order?.orderId || "order"}.pdf`);
  };

  return (
    <>
      <button className="invoice-btn" onClick={downloadInvoice} type="button">
        📄 Download Invoice
      </button>

      <div className="invoice-canvas-host" aria-hidden="true">
        <div ref={invoiceRef} className="invoice-container">
          <div className="invoice-header">
            <div className="brand">
              <h2>InstaBuy</h2>
              <p>Smart shopping, fast delivery</p>
            </div>

            <div className="invoice-title">
              <h1>INVOICE</h1>
              <p>Invoice #: {order?.orderId}</p>
              <p>Date: {order?.orderDate ? new Date(order.orderDate).toLocaleDateString("en-IN") : "—"}</p>
            </div>
          </div>

          <div className="invoice-info">
            <div>
              <h4>Invoice to:</h4>
              <p>{order?.shippingAddress || "N/A"}</p>
              <p>Phone: {order?.phone || "N/A"}</p>
              <p>Payment: {order?.paymentStatus || "N/A"}</p>
              <p>Status: {order?.orderStatus || "N/A"}</p>
            </div>
          </div>

          <table className="invoice-table">
            <thead>
              <tr>
                <th>No</th>
                <th>Item</th>
                <th>Qty</th>
                <th>Price</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item, index) => (
                <tr key={item.orderItemId || item.productId || index}>
                  <td>{index + 1}</td>
                  <td>{item.productName || `Product #${item.productId}`}</td>
                  <td>{item.quantity}</td>
                  <td>Rs. {formatCurrency(item.price)}</td>
                  <td>Rs. {formatCurrency(item.totalPrice || item.quantity * item.price)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="invoice-total">
            <p>Grand Total: Rs. {formatCurrency(order?.totalAmount)}</p>
          </div>

          <div className="invoice-footer">
            <p>Authorized Signatory</p>
          </div>
        </div>
      </div>
    </>
  );
}

export default Invoice;
