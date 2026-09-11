import { useAuth } from '../../context/AuthContext';
import { AdminHome } from '../../components/AdminHome';
import { ClientHome } from '../../components/ClientHome';
import { BarberHome } from '../../components/BarberHome';

export default function HomeScreen() {
  const { session } = useAuth();

  if (!session) return null;

  if (session.role === 'ADMIN') return <AdminHome />;
  return session.role === 'CLIENT' ? <ClientHome /> : <BarberHome />;
}
